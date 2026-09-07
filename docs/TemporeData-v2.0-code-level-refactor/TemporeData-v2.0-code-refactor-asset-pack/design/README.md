# 代码 / 技术设计（Design）

> 本目录收纳轻舟数据云（TemporeData）后端**架构与各子系统详细技术设计**（由原散落在 `docs/design/` 的十二篇 `temporedata-*.md` 归并重组而成）。每篇自包含、可单独阅读。

## 文档列表

| 文档 | 对应原设计 | 内容摘要 |
|---|---|---|
| [architecture.md](architecture.md) | 架构与功能设计 | 总体架构（分层 / 模块 / 进程 / 集成）、Maven 模块结构、7 业务域、前端架构、横切能力、数据模型与治理要点、运行模式、技术栈 |
| [module-datasource.md](module-datasource.md) | datasource-plugins 模块设计 + datasource 插件化 v2.x + 连接池方案与血缘（连接池部分） | 数据源插件化（聚合父 + SPI）、连接池选型（HikariCP vs Druid）、多数据源血缘解析（方言路由方向）、上传驱动（Maven GAV） |
| [module-sql-parser.md](module-sql-parser.md) | SQL 解析器与 Druid 定位 + 单一 SQL 解析器 Druid 化重构 | SQL 解析器现状、Druid 定位澄清（`/druid` 是连接池监控非解析器）、Druid 统一化重构（方言→DbType 注册表、SchemaStatVisitor、SPI 方言扩展）已落地 |
| [module-metadata.md](module-metadata.md) | 元数据管理模块自动化设计 | 真实 JDBC 采集、表/列分层模型、采集管线、增量 checksum、后处理管道（血缘/分级/脱敏/质量/目录联动）、MetaCoordinator、P0-P5 落地 |
| [module-lineage.md](module-lineage.md) | 数据血缘模块（后端）重新设计 + 连接池血缘解析（血缘部分） | 血缘模块后端：富语义边模型、双读路径（精确组装 + 图探索）、写路径、权限/域过滤、API 契约、/api/lineage 端点、前端承接 |
| [module-security.md](module-security.md) | 数据访问控制与动态脱敏执行链 | GovernedSqlExecutor 三阶段管道（授权决策 / SQL 下推 / 流式兜底）、大数据量有效性、脱敏器、Concluded by P0-P3 落地 |
| [infra-bom.md](infra-bom.md) | BOM 统一依赖管理 | BOM 单一版本出口、分组清单、各模块接入、部署"统一添加执行模块"、迁移步骤、升级维护规约 |
| [runtime-jar.md](runtime-jar.md) | 运行模式重设计（免重复打 jar） | 开发/发布分离、spring-boot:run + DevTools、前端 dist 外部静态、Makefile 一键运行、验收 |
| [asset-inventory.md](asset-inventory.md) | 项目资产清单（非数据库表） | 非表资产总览：模块 / Java / 前端 / 迁移脚本 / CI / 文档 / 运行态资产索引 |
| [source-tree.md](source-tree.md) | —（引用参考） | 完整源码树：后端各模块 `src/main/java/**/.java`（约 563）+ 前端 `temporedata-ui/src/**`（约 156 文件），供代码浏览/模块定位 |

---

## 模块关系图（文本）

```
                    ┌────────────────────────────────────────────────────┐
                    │          temporedata-server (启动装配/fat jar)      │
                    │  WebMvcConfig(SPA/外静态) · Flyway · application.yml│
                    └───────────────────┬────────────────────────────────┘
                                        │ depends-on
                    ┌───────────────────▼────────────────────────────────┐
                    │        temporedata-service (业务域 7 + 治理/血缘)    │
                    │  sys/dev/ops/gov/svc/asset/integration               │
                    │  · druid parser·血缘引擎·元数据·脱敏执行链·数据源元数据│
                    └───┬───────────┬────────────┬────────────┬───────────┘
                        │           │            │            │
                 ┌──────▼──┐  ┌─────▼────┐  ┌────▼────┐  ┌────▼───────────┐
                 │ common  │  │   api    │  │ support │  │  security       │
                 │ 工具/锁 │  │契约DTO/SPI│  │缓存/异常 │  │ JWT/RBAC/Trace  │
                 └─────────┘  └────┬─────┘  └─────────┘  └────────────────┘
                                   │ depends-on (SPI: DatasourceProcessor/
                                   │          SqlDialectContribution)
                    ┌──────────────▼─────────────────────────────────────┐
                    │   temporedata-datasource-plugin (聚合父, Dolphin...)│
                    │   mysql/postgresql/clickhouse/oracle/doris/         │
                    │   starrocks/hive/oceanbase (各一插件模块)           │
                    └─────────────────────────────────────────────────────┘
   版本唯一出口:  temporedata-bom (dependencyManagement, 所有模块 import)
```

### 依赖方向（防循环三原则）

1. **契约与实现分离**：`temporedata-api` 持对外契约（DTO、`BaseResponse`、SPI、`Dialect`/`DatasourceType`），实现放 `temporedata-service`。
2. **横切底座下沉**：异常、分页 `PageRes`、缓存、分布式锁、校验在 `common / support`。
3. **数据源可插拔**：连接 / 方言 / 驱动全部由 `datasource-plugin` 插件提供；新增数据源"只加插件 + 注册方言，不重写核心"。

版本约束：所有模块引用 `temporedata-bom`（import scope）取得依赖版本；`bom` 需 `mvn install` 入库后方可被消费。

---

## 依赖顺序与构建

```text
temporedata-bom（居首，mvn install 入库）
  → common / api / support / security
    → service
      → datasource-plugin（聚合父 + 插件）
        → server（fat jar）
```

- 构建：`mvn -DskipTests install`（全量）或 `mvn -pl temporedata-server -am package`（单服务）。
- 安全门禁：`-Psecurity`（SpotBugs/OWASP/CycloneDX）；`-Pinclude-engine-*`（引擎内嵌 tar 包）。