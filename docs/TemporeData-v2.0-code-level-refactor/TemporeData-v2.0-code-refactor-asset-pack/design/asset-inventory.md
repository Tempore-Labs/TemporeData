# 项目资产清单（非数据库表部分）

> 梳理整个项目中**除数据库表之外**的全部组成（代码、模块、前端、构建、部署、CI/CD、文档、脚本、运行态资产），作为工程盘点与运维索引。数据库表本体见各表 DDL / Flyway 迁移，本文只列"脚本类/代码类"资产。

## 1. 总体规模

| 类型 | 数量/说明 |
|---|---|
| 顶层模块 | 9（8 后端 + 1 前端） |
| Java 源文件 | ~560 |
| 前端 页面/JS | 59 个 `.vue` 功能页 + `api/modules/stores/composables` 若干 `.js` |
| Flyway 迁移脚本 | 28（`V1`–`V29`，含 `V29__data_api_sql_text` 等修复性迁移） |
| CI/CD workflow | 3（ci / release / security） |
| 设计文档 | （`docs/` 下，见 [README.md](../README.md)） |
| bin 脚本 | 3（build/dev/dev-ui） |
| 开源/工程文件 | LICENSE/NOTICE/README/CHANGELOG/SECURITY/CONTRIBUTING/.editorconfig/.gitignore/.dockerignore 等 |

---

## 2. 根目录工程资产

- 构建：`pom.xml`（Maven 聚合，groupId `org.temporedata`）、`Makefile`（build/test/run/dev/dev-ui/docker/release）、根 pom 的 `-Psecurity`（SAST/SCA/SBOM）与 server 的 `-Pinclude-engine-*`。
- 质量：`spotbugs-exclude.xml`（SAST 排除规则）。
- 部署：`Dockerfile`、`docker-compose.yml`（MySQL + 应用）。
- 开源合规：`LICENSE`、`NOTICE`、`README.md`、`CHANGELOG.md`、`SECURITY.md`、`CONTRIBUTING.md`、`.github/`。
- 目录：`uploads/`（本地文件存储，运行时）、`docs/`（文档）。

---

## 3. 后端模块资产（8 个）

| 模块 | 类型 | 承载（非表部分） |
|---|---|---|
| `temporedata-bom` | pom | 第三方依赖版本唯一出口（`dependencyManagement` + import Spring Boot BOM） |
| `temporedata-common` | jar | 工具、异常基类、`CryptoUtil`、`DistributedLockProvider` |
| `temporedata-api` | jar | 契约 DTO、`BaseResponse`/`PageRes`、SPI（`DatasourceProcessor`/`SqlDialectContribution`）、`Dialect`/`DatasourceType`、安全 `AccessPolicy`/`GovExecResult` |
| `temporedata-support` | jar | 统一异常 `GlobalExceptionHandler`、缓存 `CacheConfig`、异步执行器、`Crypto` bean |
| `temporedata-security` | jar | JWT、Spring Security 规则、TraceId 过滤器、租户上下文 |
| `temporedata-service` | jar | 核心业务 7 域，~560 Java 主体 |
| `temporedata-datasource-plugin` | pom | 数据源插件聚合父（8 子插件） |
| `temporedata-server` | jar | 启动装配、`WebMvcConfig`(SPA/CORS/外部静态)、`application.yml`、Flyway、单 jar repackage |

---

## 4. 前端资产（`temporedata-ui`）

- 页面（`views/`）：59 个功能页（含血缘 `Lineage.vue`/`LineageV2.vue`、元数据 `Meta.vue`、数据源 `Datasource.vue` 等）。
- 数据层：`api/modules/*`（按域封装）、`api/index.js`（axios 实例）。
- 状态：`stores/`（auth、lineage 等 Pinia store）。
- 配置/路由：`config/menu.config.js`（信息架构）、`router/index.js`、`lib/permission.js`（`hasPerm` + `v-perm` 指令）。
- 构建：`vite.config.js`（dev 代理 /api→8080，build→`dist`）、`package.json`。

---

## 5. 横切能力资产（非表代码）

- **安全与治理**：JWT/RBAC（`PermissionService`）、数据访问控制与脱敏（`GovernedSqlExecutor`/`SqlPlanRewriter`/`DataAccessGovernor`/`AccessPolicyResolver`）、SQL 防火墙（`SqlFirewall`）。
- **数据服务**：DataApi invoke（HMAC 签名 + 限流）、治理导出流。
- **审计**：`AuditService`（审计事件哈希链/策略/校验/导出）、异步治理审计（`GovernanceAudit`）。
- **缓存/锁**：Caffeine/Redis 双级（`CacheConfig`）、分布式锁（`MemoryDistributedLock`/`RedisDistributedLock`）。
- **可观测性**：TraceIdFilter + MDC + 日志 pattern。
- **数据源插件化**：8 插件 + SPI + Maven Resolver 动态加载驱动。
- **SQL/血缘/质量**：`DruidSqlParser`、`SqlDialectRegistry`、`SqlLineageParser`、`MetaCollector`/元数据后处理管道、质量规则引擎。
- **文件存储**：`FileStorageProvider`（local/S3）、上传压缩/图片优化/小文件打包。

---

## 6. 数据库相关"脚本类"资产（非表本体）

- Flyway 迁移：`temporedata-server/src/main/resources/db/migration/V{n}__*.sql`（28 例，`V1`–`V29`；血缘增强 `V31__lineage_enhance.sql`、查询治理 `V27__query_governed_result.sql`、访问策略 `V22__access_policy`、元数据分层 `V23`/`V24`/`V25`、数据源插件 `V17__datasource_plugin`/`V19__datasource_plugin_maven` 等）。
- Flyway 配置：`spring.flyway.*`（baseline-on-migrate、locations）。
- 注意：业务表以 `zy_*` 命名；血缘增强表为 `temporedata_*`；本文不含表数据。

---

## 7. CI/CD 与工程质量资产

- `.github/workflows/ci.yml`（编译+测试+前端 build+docker）、`security.yml`（SCA/SBOM/SAST + 测试门禁 + SARIF）、`release.yml`（打 tag 发版镜像）。
- 安全门禁：根 pom `-Psecurity`（SpotBugs/OWASP dependency-check/CycloneDX）。
- 部署流水线脚本：`bin/build.sh`（前端→dist→内嵌 jar）、`bin/dev.sh`（spring-boot:run + Vite dev）、`bin/dev-ui.sh`（spring-boot:run + dist watch）。
- 血缘回归脚本：`scripts/lineage-walkthrough.cjs`（Chrome 无头 + CDP 走查）。

---

## 8. 文档资产

- 设计文档 `docs/design/*.md`（本文档所在层）：架构与功能、运行模式重设计、数据源插件化 + 连接池、SQL 解析器 Druid 化、元数据自动化、数据血缘（后端）、数据访问控制与脱敏、BOM 统一依赖管理（详见 [README.md](README.md)）。
- UI 文档 `docs/ui/*.md`：通用设计原则、血缘 UI。
- 产品文档 `docs/product/*.md`：overview。
- 项目级：`README.md`、`CHANGELOG.md`、`CONTRIBUTING.md`、`SECURITY.md`、`docs/README.md`。

---

## 9. 运行态资产（非持久化）

- 缓存：`security_rule`/`data_api`/`datasource`/`META_TABLES`/`META_COLUMNS` 等 `CacheConfig` 命名分区（Caffeine 内存 / Redis）。
- 分布式锁：进程内 / Redis 锁（`td:lock:*`）。
- 异步队列：`governanceAuditExecutor` 单线程审计队列。
- 临时/插件：`uploads/`（本地文件）、插件驱动下载缓存目录（默认 `${java.io.tmpdir}/temporedata-plugins`）。

> 结论：除数据库表外，TemporeData 的资产 ≈「9 模块 + ~560 Java + 59 Vue + 28 迁移 + 3 CI + 3 脚本 + 开源/构建/部署文件 + 横切能力」。本文即为其索引清单。