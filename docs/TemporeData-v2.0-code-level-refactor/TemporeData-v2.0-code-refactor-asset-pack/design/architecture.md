# 架构与功能设计（Architecture）

> 本文基于当前工程代码核对后编写，是平台**总体架构、模块结构、业务功能划分与关键技术能力**的权威入口。分模块详细设计见本目录其余文档。

## 1. 定位与背景

轻舟数据云（TemporeData）是**企业级数据云平台**，覆盖大数据研发、治理、服务与企业管控。工程形态为**前后端一体的模块化单体 + 单一可执行 jar**：Vue3 前端经构建落入后端静态目录，由 Spring Boot 内嵌 Tomcat 一并提供。

---

## 2. 总体架构

```
                    ┌────────────────────────────────────────────┐
                    │  前端单页应用 temporedata-ui (Vue3)         │
                    │  Vite / Pinia / Vue Router / Element Plus   │
                    │  59 views, menu.config + router + perms     │
                    └───────────────┬────────────────────────────┘
                                    │ 生产:静态落入 server/static; 开发:Vite 代理 /api→8080
                                    ▼
              ┌───────────────────────────────────────────────────────────────┐
              │  统一安全入口 temporedata-security                            │
              │  JWT 认证 + Spring Security 规则 + TraceId 可观测性过滤器      │
              └───────────────┬───────────────────────────────────────────────┘
                              ▼
        ┌───────────────────────────────────────────────────────────────────┐
        │  后端模块化单体 temporedata-service（业务域编排）                    │
        │  Controller → Service(事务) → Repository(Spring Data JPA) → MySQL │
        │  7 业务域包: sys / dev / ops / gov / svc / asset / integration    │
        └───────┬───────────────┬───────────────────────┬──────────────────┘
                │               │                       │
                ▼               ▼                       ▼
   temporedata-common     temporedata-api           temporedata-support
   (工具/异常/加密/锁)      (契约/DTO/SPI/方言)        (统一异常/分页/缓存/异步)
                │               │
                ▼               ▼
        temporedata-datasource-plugin（聚合父 + 数据源插件子模块, SPI 驱动加载）
                │
                ▼
        temporedata-server（启动装配 / Flyway 迁移 / 前端静态 / 单 jar repackage）
```

### 核心分层原则

- **契约与实现分离**：`temporedata-api` 持有对外契约（DTO、`BaseResponse`、SPI、`Dialect`、`DatasourceType`），实现放 `temporedata-service`。
- **横切底座下沉**：异常契约、分页 `PageRes`、缓存、分布式锁、校验在 `common / support`。
- **安全硬约束前置**：JWT + Spring Security 在 `temporedata-security` 统一拦截，业务代码不自行处理认证。
- **数据源可插拔**：连接 / 方言 / 驱动全部由 `temporedata-datasource-plugin` 插件提供，服务端经 SPI + Maven Resolver 动态加载。

---

## 3. Maven 模块结构

| 模块 | packaging | 职责 |
|---|---|---|
| `temporedata-bom` | pom | 全工程第三方依赖版本唯一出口（import Spring Boot BOM + 覆盖平台版本） |
| `temporedata-common` | jar | 基础设施：工具、`CryptoUtil`、异常基类、`DistributedLockProvider` |
| `temporedata-api` | jar | 契约：DTO、`BaseResponse`、`PageRes`、SPI（`DatasourceProcessor`/`SqlDialectContribution`）、`Dialect`/`DatasourceType`、安全策略 `AccessPolicy` |
| `temporedata-support` | jar | 统一异常处理器、缓存配置（Caffeine/Redis）、异步执行器 |
| `temporedata-security` | jar | JWT、Spring Security 规则、TraceId/请求上下文 |
| `temporedata-service` | jar | 核心业务（7 业务域） |
| `temporedata-datasource-plugin` | pom | 数据源插件聚合父（DolphinScheduler 风格） |
| `temporedata-datasource-{mysql,postgresql,oracle,clickhouse,hive,oceanbase,doris,starrocks}` | jar | 各数据源插件（连接串/方言/JDBC 驱动） |
| `temporedata-server` | jar | 启动装配、Flyway 迁移、前端静态、单 jar repackage |
| `temporedata-ui` | —(Node) | Vue3 前端 |

> 依赖顺序（防循环）：`bom` 居首并需 `mvn install` 入库 → `common/api/support/security` → `service` → `datasource-plugin*` → `server`。

---

## 4. 后端业务域与功能清单

| 域 | 包 | 职责 | Controllers |
|---|---|---|---|
| sys | `modules.sys` | 用户/角色/租户/组织、认证（JWT）、菜单/权限、消息中心、通知、Agent 助手、设置、免密登录、视图、偏好、登录/行为审计源 | 12 |
| dev | `modules.dev` | 工作流 DAG 与运行时（`workflow`：runner/JdbcSupport/SqlLineageParser/SqlFirewall/DruidSqlParser）、SQL 查询（`query`：治理执行/导出）、调度、发布审批、依赖/函数/全局变量/资源、权限中心（RBAC `perm`） | 14 |
| ops | `modules.ops` | 监控/集群/容器/引擎/HA/告警/基线/心跳/Audit、变更审计、GitOps、数据接入（ingestion）、同步、实时 | 13 |
| gov | `modules.gov` | 元数据管理（`meta`：自动采集/增量/变更/后处理管道）、血缘（`lineage` + `catalog`）、数据质量（`quality`）、数据访问控制与动态脱敏（`security`）、敏感数据（`sensitive`）、审计中心 | 10 |
| svc | `modules.svc` | DataApi 接口（管理 + `invoke` 调用方认证）、接口日志、黑白名单、数据报表、表单 | 6 |
| asset | `modules.asset` | 数据中心、指标、标签、我的数据（MyData）、权限审批 | 5 |
| integration | `modules.integration` | 数据源（`datasource`，含驱动/插件）、密钥（`secret`）、文件（`file`：上传/压缩/对象存储）、驱动管理（`driver`）、数据安全规则（掩码 `security`） | 5 |

---

## 5. 前端架构

- 栈：Vue3（`<script setup>`）+ Vite + Element Plus + Pinia + Vue Router + Axios + ECharts / G6。
- 目录：`views/`（59 页）、`api/modules/`（按域封装）、`stores/`（auth 等）、`config/menu.config.js`（信息架构：7 工作区 + 管理中心 + 个人空间）、`router/index.js`、`lib/permission.js`（P3-11 `hasPerm` + 全局 `v-perm` 指令）。
- 构建：`npm run build` 输出至 `temporedata-server/.../resources/static`；开发 `npm run dev`（5174）代理 `/api→:8080`。

---

## 6. 关键技术能力（横切）

| 能力 | 方案 / 实现 |
|---|---|
| 认证授权 | JWT（jjwt）+ Spring Security；RBAC 资源级鉴权（`PermissionService`：DENY 优先/通配 `*`/继承） |
| 数据访问控制与脱敏 | `GovernedSqlExecutor` 统一引擎：策略解析(`AccessPolicyResolver` 两级缓存) → SQL 下推(`SqlPlanRewriter`，MySQL 族) → 结果流式治理(`DataAccessGovernor`)；fail-closed；`SELECT *` 规避明文泄露 |
| 数据服务鉴权/限流 | DataApi `invoke`：`X-API-Key` + HMAC-SHA256 签名 + 时间窗 + 按 key 限流 |
| 安全扫描 | `-Psecurity`：SpotBugs(SAST)/OWASP(SCA)/CycloneDX(SBOM) + `security.yml` 门禁 |
| 审计 | `AuditService`：单表 `audit_event` + SHA-256 哈希链（seq/prevHash）+ 策略分级 + 校验/导出；治理访问异步审计 |
| 缓存 | Caffeine(L1)/Redis(L2) 双级，`CacheConfig` 命名分区 + TTL；策略缓存事件失效 |
| 分布式锁 | `DistributedLockProvider`（memory/redis），z.lock.* |
| 可观测性 | RequestId/TraceId（`TraceIdFilter` + MDC）+ 日志 pattern 带 traceId/userId/tenantId |
| 数据源插件化 | `temporedata-datasource-plugin` 8 插件 + SPI（`DatasourceProcessor`/`SqlDialectContribution`）+ Maven Resolver 动态拉驱动 |
| SQL 解析/血缘 | `DruidSqlParser`（多方言）+ `SqlDialectRegistry` + `SqlLineageParser` + `SqlFirewall`(Druid Wall)；表级 + 列级血缘 |
| 文件/存储 | `FileStorageProvider`（local/S3 兼容）；压缩(gzip/zstd)、图片优化、小文件打包 |
| 迁移 | Flyway（baseline 接纳存量 + 版本化 `V{n}__*.sql`） |

---

## 7. 数据模型与治理要点

- 平台库 MySQL：业务表以 `zy_` 前缀（用户/角色/租户/工作流/元数据/资产/质量/脱敏规则/审计等）；数据源元数据 `zy_datasource`；插件 `zy_datasource_plugin`。
- 元数据：`zy_meta_table / zy_meta_column / zy_meta_sync_log / zy_meta_change`（采集→checksum 增量→变更集→后处理）。
- 血缘：`zy_workflow_lineage`（表级）+ `zy_column_lineage`（列级）；血缘增强为 `temporedata_lineage / temporedata_column_lineage / temporedata_workflow_lineage / temporedata_meta_table`。
- 治理：`zy_mask_rule`（库·表·列脱敏）、`zy_access_policy`（策略物）、`audit_event`（审计链）。
- 命名规约：保留字段处理（`sql` 用反引号等）；`id` 统一 varchar(36)；`temporedata_*` 为血缘增强表命名。

---

## 8. 运行模式

temporedata 采用**前后端一体交付**：`npm run build` 将前端产物输出到 `temporedata-server/src/main/resources/static`，`spring-boot-maven-plugin` 打包为**单一可执行 jar**（内嵌 Tomcat + 前端静态）。

### 运行前置依赖与默认端口

| 组件 | 版本/端口 | 必需 |
|---|---|---|
| JDK | 11 | 是 |
| Node.js | 20 | 开发 / 构建必需 |
| Maven | 3.6+ | 后端构建 |
| MySQL | 3306 | 平台库（Flyway 自动迁移） |
| Redis | 6379 | **可选**（`spring.cache.type=redis` 或 `temporedata.lock.provider=redis` 时才需要；默认 caffeine + 内存锁） |
| 后端内嵌 Tomcat | 8080 | 生产 / 运行 |
| Vite dev server | 5174 | 仅开发 |

环境变量：`DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD`、`REDIS_*`、`TEMPOREDATA_JWT_SECRET/TEMPOREDATA_CRYPTO_KEY`、`TEMPOREDATA_CACHE_TYPE`、`TEMPOREDATA_LOCK_PROVIDER`、`APP_FILE_*`。

### 模式一：开发（热更新，无需打 jar）

- 后端：IDE 运行 `TemporeDataApplication` 或 `mvn -pl temporedata-server spring-boot:run`；源码/classes 启动，Flyway 自动迁移。依赖 `temporedata-bom` 已 `mvn install` 入库。
- 前端：`cd temporedata-ui && npm run dev`（Vite @5174，`/api` → 8080）。
- 联调拓扑：浏览器访问 5174，API 经 Vite 代理落到 8080；前后端各自独立进程。

### 模式二：生产 / 部署（单 jar）

```bash
cd temporedata-ui && npm ci --no-audit --no-fund && npm run build   # 产物 → server/static
mvn -pl temporedata-server -am package -DskipTests                    # → target/temporedata.jar（repackage）
java -jar temporedata-server/target/temporedata.jar                    # 进程监听 8080，API + 前端静态
# 可选安全扫描门禁替代 package：mvn -Psecurity verify
```

### 构建 profile（可选）

| profile | 位置 | 作用 |
|---|---|---|
| `-Psecurity` | 根 pom | SpotBugs(SAST) + OWASP(SCA) + CycloneDX(SBOM) + 测试，CI/发布前 |
| `-Pinclude-engine-spark/flink/hadoop/calcite` | `temporedata-server` | 把 provided 引擎以 compile 打入 fat jar，一体化部署 |

> 开发/迭代免重打包 jar 的完整设计见 [runtime-jar.md](runtime-jar.md)。

---

## 9. 技术栈清单

- **后端**：Java 11、Spring Boot 2.7、Spring Data JPA、Flyway、Spring Security、Springdoc、Druid（SQL 解析）、HikariCP；构建 Maven + BOM。
- **前端**：Node 20、Vue 3、Vite、Element Plus、Pinia、Vue Router、Axios、ECharts、G6。
- **中间件**：MySQL；可选 Redis（缓存 / 分布式锁）。
- **DevOps**：GitHub Actions（ci / security / release）、Docker。

---

## 10. 与各模块设计的关系

| 能力 | 详细设计 |
|---|---|
| 连接池 HikariCP 选型 + 数据源 SPI | [module-datasource.md](module-datasource.md) |
| SQL 解析 / Druid 统一 | [module-sql-parser.md](module-sql-parser.md) |
| 元数据自动化采集 | [module-metadata.md](module-metadata.md) |
| 数据血缘（后端） | [module-lineage.md](module-lineage.md) |
| 数据访问控制与脱敏执行链 | [module-security.md](module-security.md) |
| 依赖版本统一 | [infra-bom.md](infra-bom.md) |
| 前端血缘 UI | [../ui/lineage.md](../ui/lineage.md) |