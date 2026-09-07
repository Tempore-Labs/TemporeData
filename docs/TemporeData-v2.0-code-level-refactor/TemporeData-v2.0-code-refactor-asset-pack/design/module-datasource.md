# 数据源模块设计（Datasource）

> 归并原三篇：《temporedata-datasource-plugins 模块设计》、《temporedata 数据源插件化设计 v2.x》、《连接池方案与多数据源 SQL 血缘解析设计》。本文明确：① 数据源插件化的**最终形态**（v2.x，SPI 并入 `temporedata-api`）；② 连接池选型（HikariCP）；③ 多数据源血缘解析的方言化方向（已由 Druid 统一解析器落实，详见 [module-sql-parser.md](module-sql-parser.md) 的"血缘部分"）。

---

## 1. 定位与背景

数据源接入是平台连接异构数据库的基础能力，演进目标是 **"一库一模块、SPI 可拔插"**（DolphinScheduler 风格）。经历多次迭代：

| 版本 | 内容 |
|---|---|
| v1.1 | 新建 `temporedata-datasource-plugins`，把根下数据源实现整体迁入 |
| v2.0 | 建 `temporedata-datasource-plugin` 聚合父（api + 三插件 + manager） |
| v2.1 | manager 并入 `service`（`integration.datasource` 包，包名不变）；聚合父只留插件 |
| **v2.2（最终）** | **SPI 并入 `temporedata-api`**（`org.temporedata.api.datasource`），删除独立 `temporedata-datasource-api` 模块；内置方言扩到 8 个；外部插件（PluginManager + `zy_datasource_plugin`/`/api/driver`）落地 |

---

## 2. 目标模块结构（v2.2 最终布局）

```
temporedata/                                   根
├── temporedata-api                (DTO契约 + 数据源 SPI：org.temporedata.api.datasource)
├── temporedata-common / temporedata-support / temporedata-security(现有)
├── temporedata-service           (业务服务域)
│    └── modules/integration/datasource/       元数据 CRUD + DriverPluginController(/api/driver)
├── temporedata-datasource-plugin/              聚合父：只容纳各库插件
│   ├── temporedata-datasource-mysql
│   ├── temporedata-datasource-postgresql
│   ├── temporedata-datasource-clickhouse
│   ├── temporedata-datasource-oracle
│   ├── temporedata-datasource-doris
│   ├── temporedata-datasource-starrocks
│   ├── temporedata-datasource-hive
│   └── temporedata-datasource-oceanbase
└── temporedata-server
```

### 职责划分

| 归属 | 包/位置 | 职责 |
|---|---|---|
| `temporedata-api` | `org.temporedata.api.datasource.*` | 数据源 SPI：`DatasourceProcessor` / `DatasourceParamDTO` / `DatasourcePluginContext` / `PluginManager` / `@DatasourcePlugin`，以及 `DatasourceType` 与 `Dialect` |
| `temporedata-datasource-{type}` | 插件聚合父下 | 各库实现：一个 Processor + 自带 JDBC 驱动 |
| `temporedata-service` | `integration.datasource` 包 | 元数据 `zy_datasource` CRUD + 连接测试 + `/api/driver` 插件管理 |

---

## 3. 依赖关系（避免循环、进入 fat jar）

```
temporedata-datasource-{type}   -- 依赖 temporedata-api(SPI) + 各自 JDBC 驱动
temporedata-service  -->  temporedata-api + 八个插件模块(mysql/pg/clickhouse/oracle/doris/starrocks/hive/oceanbase)
temporedata-server   -->  temporedata-service
```

> ⚠️ **Maven 机制**：聚合父 `packaging=pom` **不可在自身 `<dependencies>` 中引用其 `<modules>` 子模块**——子模块 `parent` 是聚合父，会继承该依赖从而形成自引用循环，Maven 报错。因此聚合父**仅做 `<modules>` 排序**，内置插件由 `temporedata-service` 直接声明依赖带入运行时。

---

## 4. SPI 契约（`org.temporedata.api.datasource` 包）

- `DatasourceType`：`MYSQL / POSTGRESQL / CLICKHOUSE / ORACLE / DORIS / STARROCKS / HIVE / OCEANBASE`。
- `DatasourceProcessor`：`type()/dialectName()/defaultPort()/paramClass()/buildConnection()/getConnection()/check()/queryDatabases()/queryTables()`。
- `AbstractDatasourceProcessor`：通用 URL 构建 + 驱动加载 + 连接 + 连通性测试。
- `DatasourceParamDTO`（抽象）+ `GenericDatasourceParamDTO`（通用 RDBMS）。
- `DatasourcePluginContext`：Spring 自动收集内置 Processor；`register/unregister` 支持外部插件。
- `@DatasourcePlugin`：注册元数据注解。

每个插件结构一致：`{Type}Processor.java` + 自带驱动（包 `org.temporedata.datasource.{type}`）。新增一个库 = 建一个插件模块 + 聚合父 `<modules>` 加一项 + `service/pom.xml` 加一项依赖；service 与其余插件零改动。

---

## 5. 连接池方案（HikariCP 选型）

### 池实现候选对比

| 方案 | 活跃度 | 性能 | 监控/泄漏检测 | Spring Boot 集成 |
|---|---|---|---|---|
| **HikariCP** | 高（Spring Boot 默认） | 最优 | 连接泄漏检测、等待统计 | 开箱即用 |
| **Druid**（保留连接池、关控制台） | 高（阿里） | 良好 | 最丰富（慢SQL/审计/防火墙） | starter 现成 |
| Tomcat JDBC Pool | 高 + JMX | 良好 | 内置 | starter 可选 |
| Commons DBCP2 / C3P0 / Vibur | 中/低 | 一般 | 有/弱 | 手动 |

> 前提：Spring 的 `javax.sql.DataSource` 是抽象层，池实现可互换——改 `spring.datasource.type` 一行 + 调整依赖即可，业务代码零改动。

### 结论（已采用路线 A）

- **首选路线 A（HikariCP）**：`spring.datasource` 走 Spring Boot 默认 HikariCP，Druid **只作为 SQL 解析库、不再当连接池**（移除 `druid-spring-boot-starter`、移除 `/druid` 监控控制台）。Closure 上与 SQL 解析器定位一致。
- 若企业强依赖 Druid SQL 监控/审计指标，可选路线 B（保留 Druid 连接池并收敛监控）；其余池（Tomcat/DBCP2/C3P0/Vibur）不推荐。

---

## 6. 元数据管理（并入 `temporedata-service`）

`org.temporedata.modules.integration.datasource` 包（不变）下：
- `entity/DatasourceEntity.java`（含 `database` 保留字处理）
- `repository/DatasourceRepository.java`
- `service/DatasourceService.java`（经 `DatasourcePluginContext` 解析，不感知具体库）
- `controller/DatasourceController.java`（`/api/datasource/**` + `/plugins/types`）

**DatasourceService** 要点：`buildJdbcUrl` 由 `plugin=registry.resolve(type)` 拼模板（`plugin.urlTemplate()` + 参数追加）；`testConnection` 用 `Class.forName(plugin.driverClass())` + `DriverManager`，密码 `CryptoUtil.decrypt`；`create/update` 时 type 无可解析插件 → 拒绝（400）。

---

## 7. 前端调整

数据源插件**不设独立菜单/路由/页面**，作为「数据资产 → 数据源」页的附属能力并入：

- 「数据源」页（`views/Datasource.vue`）标题旁提供「驱动插件」按钮，弹出**只读插件列表**对话框。
- 数据来自 `GET /api/datasource/plugins`（[DatasourcePluginInfo](../temporedata-api/src/main/java/org/temporedata/api/datasource/DatasourcePluginInfo.java)），随 SPI 注册表动态展示：类型 / 名称 / 默认端口 / 来源（内置|上传）。
- 支持关键词搜索 + 来源筛选（前端过滤）；数据源类型下拉取 `/api/datasource/plugins/types`。
- `api/modules/datasourcePlugin.js` 仅保留 `list()`。

---

## 8. 上传驱动：Maven 坐标动态解析依赖

将"上传驱动"从本地 jar 路径升级为 **Maven 坐标（GAV）运行时拉取驱动及其传递依赖**（Aether / `org.apache.maven.resolver`，与 DolphinScheduler 同机制）。

### 组件

| 组件 | 位置 | 职责 |
|---|---|---|
| `MavenDependencyResolver` | `service` 内 `integration.datasource.driver` | 由 GAV → 解析并落盘依赖文件 |
| `PluginManager.load(List<File> jars)` | `temporedata-api` 的 `datasource.context` | 隔离加载、SPI 发现、注册/卸载 |
| `DriverPluginService / /api/driver` | `service` | 编排：GAV 解析 → 加载 → 入库；列表/删除 |
| 前端「新增插件」表单 | `Datasource.vue` | 填 GAV（group/artifact/version）+ 名称 |

### 数据流

```
前端(新增插件:GAV+名称) ──POST /api/driver {mvnGroup,mvnArtifact,mvnVersion,name}──►
DriverPluginService → MavenDependencyResolver.resolve (maven-resolver 解析构件+传递依赖)
  → 下载到缓存 ${user.home}/.temporedata/plugins（可配 temporedata.plugin.maven.cache，默认 ${java.io.tmpdir}/temporedata-plugins）
  → PluginManager.load(jars)：URLClassLoader 隔离加载 → ServiceLoader 发现 DatasourceProcessor
  → context.register(type, processor)，记录 zy_datasource_plugin(builtin=false) → 列表动态展示
```

### 表调整（Flyway `V19__datasource_plugin_maven.sql`）

```sql
ALTER TABLE zy_datasource_plugin
    ADD COLUMN mvn_group   VARCHAR(128) DEFAULT NULL,
    ADD COLUMN mvn_artifact VARCHAR(128) DEFAULT NULL,
    ADD COLUMN mvn_version  VARCHAR(50)  DEFAULT NULL;
```

### 运行注意

- `org.apache.maven.resolver:maven-resolver:1.9.x` 的类仍在 `org.eclipse.aether` 命名空间（非 `org.apache.maven.resolver`），import 用 `org.eclipse.aether.*`。
- 远程仓库默认阿里云镜像，可用 `temporedata.plugin.maven.repos`（逗号分隔）改为内网 Nexus；无 HTTPS 出网/沙箱受限 JVM 中 TLS 握手可能被终止（`SSL peer shut down incorrectly`），生产中需配置可达仓库；本地 jar 路径上传保留为兜底。

---

## 9. 多数据源 SQL 血缘解析（方言化方向）

> 该方向的完整落地见 [module-sql-parser.md](module-sql-parser.md)（单一 Druid 解析器 + `SqlDialectRegistry` 方言→DbType 注册表）。此处保留方向性设计：**按方言路由解析器，血缘带方言落库**。

- 方言模型：`Dialect` = `DatasourceType` 的解析视角映射；数据源类型含 6~8 种，支持插件 SPI 动态扩展。
- 血缘输出：表级血缘（每个 SQL 节点 → 1 目标表 + N 源表）；列级血缘为可选增强。
- DAG 输出：`WorkflowLineageService.graph(workflowId)` 输出 `{nodes:[task|table], edges:[READ|WRITE|DEP]}`（前端零改动）。
- 集成点：`WorkflowLineageService.sync` 与 `WorkflowNodeEntity` 增加 `datasourceType` 字段（实际已并入方言注册）。