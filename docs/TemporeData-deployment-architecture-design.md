# TemporeData 核心部署架构与数据表部署技术方案（v2.1）

> 学习参考：Apache DolphinScheduler「去中心化 / 分布式 / 高可用 / 微内核插件化」调度架构，
> 以及 qData「展现 → 网关 → 应用 → 计算引擎 → 数据源」五层分层模型。
> 本文档据此，结合 TemporeData 现有真实代码结构与运行方式，输出 **目标部署架构/技术方案** 与
> **数据表在系统中的部署技术方案**，并标注当前现状与演进路线。

- 版本：v2.1
- 编制：架构与交付
- 范围：核心部署架构、调度解耦、数据表部署、层间通信、演进路线
- 现状基线：v3.0（平铺式 Maven 多模块 Spring Boot 单体 + Vue3 前端）

---

## 0. 现状盘点（重设计的起点）

| 维度 | TemporeData 现状 | 说明 |
|---|---|---|
| 整体形态 | **单体可执行 jar** `temporedata-server/target/temporedata.jar`，`java -jar` 单实例 :8080 | 全部业务模块打包进一个 Spring Boot 应用 |
| 模块结构 | 平铺 Maven：`api/common/support/service/integration/metadata/quality/asset/contract/security/gateway/development/server` | 已具备模块化、插件化底座 |
| 前端 | `temporedata-ui`（Vue3 + Vite），`vite build → dist` + Nginx 反代；dev 走 Vite :5173 代理 | 匹配 qData 展现层 |
| 调度内核 | 内置 `SchedulerCoordinator` + `TaskScheduleService` + `InProcessTaskDispatcher`，带 `DistributedLock`(scheduler:scan) 去中心化争抢 | DolphinScheduler Master 雏形 |
| 任务执行 | `InProcessTaskDispatcher` 真实派发 SQL/SCRIPT/SYNC，`WorkflowRunner` 跑 DAG，`EngineLauncher` 提交 Flink/Spark，`SqlNodeExecutor` JDBC | DolphinScheduler Worker 雏形 |
| 数据表管理 | **Flyway V1–V63** 版本化迁移，启动自动应用；`flyway_schema_history` 记录版本与 checksum；不启用 hbm2ddl | 唯一 schema 权威 |
| 主键 | 绝大多数 `Hibernate uuid2`(36字符)；Enterprise 简表 `IDENTITY(bigint)` | 分表/分布式写入友好 |
| 多租户 | 业务表带 `tenant_id`；`TenantContext` 注入，缺省 `DEFAULT` | 租户隔离 |
| 数据访问 | JPA Repository；SQL 经 `GovernedSqlExecutor`(治理链：列裁剪+脱敏) | 治理执行链 |
| 通信 | 进程内直接调用；跨服务/跨节点无 Netty | 目标态需引入 |

---

## 1. 核心部署架构（目标态）

### 1.1 总体拓扑

将 DolphinScheduler 的「API Server / Master 去中心化集群 / Worker 无状态集群 / Registry / Alert」与
qData 五层分层合并，落到 TemporeData 上：

```
                     +------------------------------------+
                     |   展现层  UI (Vue3, v2 features)     |
                     |   Nginx :80 / CDN   (静态 dist)     |
                     +------------------------------------+
                                    | HTTPS / WSS
                                    v
                     +------------------------------------+
                     |   网关层  temporedata-service-gateway |
                     |   JWT 鉴权 · 限流 · CORS · 转发       |
                     +------------------------------------+
                     |   [TemporeData API Server]  RESTful   |
                     +------------------------------------+
            +--------------------+---------------------+
            | 业务/治理/资产模块    |  Meta / Quality /    |
            |  dev·ops·gov·sys·svc |  Asset·Contract      |
            +--------------------+---------------------+
                                    |
                +-------------------+-------------------+
                | (去中心化锁/心跳)   | (任务命令)          |
                v                   v                   v
         +-----------+       +---------------+    +------------+
         | Database  |       |  Registry/ZK  |    | Alert(邮件/ |
         | (MySQL)   |       |  (Etcd 可选)   |    | 飞书/Webhook)|
         +-----------+       +---------------+    +------------+
           ^      ^              ^     ^
           |      +----+         |     +----+
           v           v         v          v
     +-----------+   +---------------+---------------+
     |  Master    |<->|     Worker     |  (SQL/Script/ |
     |  集群(HA)   |Netty|  集群(无状态)   |  SYNC/DAG/    |
     |  调度/容错   |   |  执行任务      |  Flink/Spark)|
     +-----------+   +---------------+---------------+
                  （可选）计算引擎 Flink / Spark
```

### 1.2 运行时组件 = TemporeData 现有类 → 目标职责

| DolphinScheduler 角色 | TemporeData 现有实现 | 目标演进职责 |
|---|---|---|
| API Server | `*.controller`（`WorkflowController`、`TaskScheduleController`、`DatasourceController`…） | RESTful 入口、权限校验、工作流 CRUD |
| Master（调度核心） | `SchedulerCoordinator`、`TaskScheduleService`、`InProcessTaskDispatcher`；`DistributedLock`(scheduler:scan) 争抢 | 命令扫描→实例化；DAG 拆解；分发；故障接管 |
| Worker（执行节点） | `InProcessTaskDispatcher` + `SchedulerTaskExecutor`（SQL/SCRIPT/SYNC）、`WorkflowRunner`、`SqlNodeExecutor`、`EngineLauncher` | 接收任务、拉起进程、回报状态与日志 |
| Registry | `org.temporedata.common.locker.DistributedLock`（内存/DB） | 心跳、上下线感知、分布式锁、选主 |
| Alert | `ops/alarm`（`AlarmService`/`AlarmBaselineService`，channel 邮件/Webhook） | 独立告警拉取调度/任务事件推送 |
| Netty 通信 | 进程内直调 | Master↔Worker 长连接推任务、回报结果 |

### 1.3 关键技术方案（5 条，对照文档提炼）

1. **去中心化 + 自动容错（Decentralized HA）**
   - Master 对等集群，无固定主备：基于 Registry 抢占分布式锁接管工作流实例（现状 `DistributedLock` 已是该模式的雏形）。
   - Master 故障转移：接管宕机 Master 未完成的 DAG，重建内存图并重新下发。
   - Worker 故障转移：Master 感知 Worker 心跳消失 → 将运行中任务标记失败并重试到健康 Worker。
   - **演进**：把单进程内的 `DistributedLock`(DB) 升级为插件化 Registry（ZK/Etcd），把「同一 JVM 内 dispatch」抽出为可跨进程的 Worker 通道。

2. **负载均衡与资源隔离**
   - Worker Group：把 Worker 划分为组（如 `sync-group`、`flink-group`），工作流/任务配置指定运行组。
   - 动态过载保护：Worker 上报 CPU/内存，Master 分发时避免压向高负载节点。
   - 多租户隔离：按真实 OS/Hadoop user 拉起任务进程（目标态）；现状以 `tenant_id` + 进程沙箱承载。

3. **微内核 + SPI 插件化**
   - 任务插件：`integration/core/etl` 已有 `Connector/DataReader/DataWriter/CdcReader/SchemaReader` SPI 骨架；任务类型（SQL/SCRIPT/SYNC/WORKFLOW）经 `TaskDispatcher` 派发 → 扩展为 Task SPI 注册表。
   - Registry 插件：抽象 `RegistryService`，支持 DB→ZK/Etcd。
   - Alert 插件：`ops/alarm` 的 channel 设计为 Alert SPI。
   - 资源存储插件：文件中心 `integration/core/file` 存储抽象 → HDFS/S3/MinIO。

4. **高吞吐通信**
   - 现状：DB 轮询 + 进程内调用 → 目标：Master↔Worker 以 **Netty 长连接**直推任务、回传日志，降低 DB 轮询压力与调度延迟。

5. **异步与缓存（目标态补充，承接 qData 持久化层）**
   - Redis：Token 缓存、分布式锁、热点字典。
   - Kafka/RabbitMQ：任务事件、日志采集、告警异步投递。

### 1.4 部署形态（现状 → 目标）

| 形态 | 现状推荐 | 目标（HA/扩展） |
|---|---|---|
| 单机直启 | `java -jar temporedata.jar` :8080（Dev/演示） | — |
| 容器化 | Docker 单容器 + Nginx | 按角色拆容器：`api`/`master`/`worker` 镜像，`docker compose` / K8s |
| 分布式 | — | Master 集群 + Worker 横向扩/缩容，Registry(ZK/Etcd) 管理 |
| 前端 | `vite build → dist` + Nginx | 同左 + CDN |

---

## 2. 数据表在系统中的部署技术方案

### 2.1 建表与迁移（Schema as Code）

- **唯一权威 = Flyway 迁移脚本**（`temporedata-server/src/main/resources/db/migration/V*.sql`，当前 V63）。
- 后端每次启动由 Flyway 自动增量应用未执行迁移；先于一版保证幂等（`MODIFY`、`CREATE IF NOT EXISTS`）。
- 不启用 `hbm2ddl auto`：实体注解不决定真实表结构，避免隐式改表引发回归。
- 新增表必须紧随 `V<next>.sql` 出道，并在 `flyway_schema_history` 登记（success=1）。

### 2.2 主键策略

| 策略 | 适用 | 说明 |
|---|---|---|
| `uuid2`（Hibernate, 36 字符） | 绝大多数业务表（`zy_*`） | 分布式写入无竞态，适合 Master/Worker 任意节点插入 |
| `IDENTITY(bigint)` | Enterprise 简表（`temporedata_metrics`、`temporedata_quality_*`） | 单库简表，整形自增 |

> 约束（已固化，`UuidIdWidthConsistencyTest`）：凡 uuid2 实体，id 列宽度必须 ≥ `varchar(36)`，
> 否则插入触发「Data too long」。V60–V63 已把全库 uuid2 表 id 统一到 36。

### 2.3 表分组与命名

| 前缀/分组 | 所属模块 | 示例 |
|---|---|---|
| `zy_*` | 历史/平铺业务表 | `zy_user`、`zy_sync`、`zy_alarm`、`zy_workflow`、`zy_task_define/instance/log` |
| `temporedata_*` | 2026 Enterprise / 数据中台 | `temporedata_dataset`、`temporedata_lineage`、`temporedata_quality_rule/gate`、`temporedata_data_contract/cost/incident` |
| `cal_*` | 调度日历 | `cal_calendar`、`cal_day_meta`、`cal_cut_time`、`biz_date_queue` |
| `res_*` | 权限资源 | `res_permission`、`res_resource` |
| `ops_*`/`t_*` | 工程/告警域 | `ops_build/repo`、`t_alert_rule/event`、`t_incident` |
| `audit_*` | 审计域 | `audit_event/policy/archive` |

### 2.4 内建列约定

- 多租户：`tenant_id varchar(36)`（多数表），由 `TenantContext` 注入；缺省 `DEFAULT`。
- 审计：`create_date_time / create_by / update_date_time / update_by`（`@CreationTimestamp`/`@UpdateTimestamp`），可选 `version`（乐观锁）。
- 类型要点：主键 `varchar(36)`；时间 `datetime/timestamp`；大对象 `TEXT`（`params_json`/`nodes_json`/SQL）；布尔 `tinyint(1)`；金额 `DECIMAL`。
- JDBC：`allowPublicKeyRetrieval=true` 适配 MySQL 8 `caching_sha2_password`。

### 2.5 数据访问部署

- **读改写路径**：Repository(JPA) 或 `GovernedSqlExecutor`（治理链：列裁剪 + 动态脱敏 + SQL 注入拦截）。
- **外部数据源**：通过 `DatasourceEntity`(`zy_datasource`) + `DatasourceService` 注册，密码 AES 加密；经 `datasource` 模块统一连接/测试。
- 任务执行的 SQL/JS 由 `SqlNodeExecutor`/`SchedulerTaskExecutor` 走 JDBC（可指定数据源）。

### 2.6 生命周期 / 归档 / 备份

- 归档表：`audit_archive` 等承接历史流水；部署时建议按表域制定保留策略（日志/实例/审计）。
- 白启/迁移/回滚：Flyway 版本化保证，必要时 `mvn flyway:repair` 校准 checksum。
- 备份：数据库容器持久化卷 + 定时逻辑备份；生产建议主从/双活（承接 DolphinScheduler 高可用理念）。

### 2.7 分布式/缓存/消息（目标态）下的数据存放

| 能力 | 存放位置 | 承接 |
|---|---|---|
| 元数据主库 | MySQL（`temporedata`） | 五层「持久化层」 |
| Token/分布式锁 | Redis（目标） | 去中心化锁/缓存 |
| 任务事件/日志流 | Kafka（目标） | 异步事件驱动、日志采集 |
| 计算编排物 | 工作流定义/实例（`zy_workflow*`、`zy_task_*`） | DolphinScheduler 命令/实例表 |
| 血缘/指标 | `temporedata_lineage`、`temporedata_metric` | 资产/血缘视图 |

---

## 3. 层间通信与容错策略

| 连接 | 现状 | 目标 |
|---|---|---|
| 展现层→网关 | Nginx 反代 `/api` → :8080 | 网关层统一入口，JWT 校验 |
| 网关→应用 | 进程内调用 | 同实例；大并发时可多实例 + K8s Service |
| Master→Worker | 进程内 `dispatch()` | Netty 长连接推送 + 结果回传 |
| 调度→数据库 | JDBC 扫命令/心跳 | 减少轮询；锁/心跳迁移 Registry |
| 应用→外部源/引擎 | JDBC / `EngineLauncher` | Connector SPI + 分布式执行器 |

---

## 4. 演进路线（Roadmap）

- **P0-可交付（单体现状）**：保持 `java -jar` + Flyway 单库；优化 `InProcessTaskDispatcher` 与调度闭环；固化主键/表宽约束（已完成单元测试）。
- **P1-容器化分层部署**：Nginx + 后端容器 + MySQL 容器 `docker compose`；前端并入。
- **P2-去中心化调度拆分**：把 Master(`SchedulerCoordinator`) 与 Worker(`SchedulerTaskExecutor`/`WorkflowRunner`) 拆为独立可部署单元；引入 `RegistryService` SPI（DB→ZK/Etcd）。
- **P3-高可用与弹性**：Master 集群 + Worker 横向伸缩；故障接管/重试；Netty 通信；Redis/Kafka 联动。
- **P4-插件化生态**：Task/Alert/Registry/ResourceStorage 全 SPI；对接真实 Flink/Spark 集群做生产提交。

---

## 5. 附录

- 现状参考文件：
  - 调度：`temporedata-service/.../dev/schedule/`（`SchedulerCoordinator`、`TaskScheduleService`、`InProcessTaskDispatcher`）
  - 执行：`.../dev/schedule/execute/SchedulerTaskExecutor`、`.../dev/workflow/runner/*`、`.../ops/real/EngineLauncher`
  - 治理：`.../gov/security/GovernedSqlExecutor`
  - 迁移：`temporedata-server/src/main/resources/db/migration/V1–V63`
- 约束文档：凡 uuid2 实体 id 列必须 ≥ `varchar(36)`（`UuidIdWidthConsistencyTest`）。