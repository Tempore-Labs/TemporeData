# 产品定位与总览（Overview）

> 本文基于当前工程代码核对后编写，作为平台**总体定位、业务能力划分与宏观模块结构**的权威入口。技术设计详见 [design 设计文档](../design/)。

---

## 1. 产品定位

**轻舟数据云（TemporeData）** 是一个**企业级数据云平台**，面向大数据场景提供一体化 **数据研发、数据资产、数据服务与企业管控**。它统一了「数据开发、资产、服务与治理」四项核心能力，并叠加企业管控（租户 / 组织 / 角色 / 安全 / 审计）。

**工程形态**：前后端一体的**模块化单体 + 单一可执行 jar**。Vue 3 前端经构建落入后端静态目录，由 Spring Boot 内嵌 Tomcat 一并提供。

```text
temporedata-ui（Vue 3 SPA）  ──构建──►  落入 temporedata-server 静态目录
temporedata-server（Spring Boot）       单个可执行 JAR（内嵌 Tomcat + 前端静态）
```

快速开始：

```bash
# 一键构建（前端 + 后端 → dist/temporedata.jar）
./tools/build.sh
# 本地容器化部署（MySQL + 应用）
docker compose -f docker/docker-compose.yml up -d --build
# 默认账号 admin / admin123（部署后请立即修改）
```

---

## 2. 7 大工作域（产品信息架构）

产品能力按七大工作区 + 管理中心 + 个人空间组织，与前端 `config/menu.config.js` 的信息架构一一对应：

| 工作区 | 核心职责 | 关键子能力 |
|---|---|---|
| **数据研发** | 数据开发主线 | 工作流（DAG / 条件分支 / 血缘）、SQL 编辑器、实时计算、离线任务、发布审批 |
| **数据资产** | 数据资产的沉淀与治理 | 数据目录、元数据、指标、标签、数据质量、数据血缘 |
| **数据服务** | 对外提供数据能力 | 接口服务（DataApi）、数据报表、表单、接口黑白名单 |
| **运行中心** | 调度与运维 | 工作流调度、告警、基线、资源监控 |
| **安全治理** | 企业数据安全管控 | 权限（RBAC）、数据分类分级、敏感数据、脱敏、审计（哈希链） |
| **管理中心** | 平台与组织管理 | 租户、组织、角色、用户、系统配置 |
| **个人空间** | 个人工作台 | 消息、个人偏好、轻舟智助（Agent 助手） |

> 研发 / 资产 / 服务 / 治理四项为产品主干，运行中心与安全治理为横切支撑，管理中心与个人空间为平台管控与个性化入口。

---

## 3. 核心能力清单

| 能力领域 | 具体能力 |
|---|---|
| **数据开发** | 工作流（DAG / 条件分支 / 血缘）、SQL、实时计算、发布审批 |
| **数据资产** | 目录 / 元数据 / 指标 / 标签 / 质量 / 血缘 |
| **数据服务** | API / 报表 / 表单 / 日志 / 黑白名单 |
| **运行中心** | 调度 / 告警 / 基线 / 资源监控 |
| **安全治理** | 权限（RBAC）/ 分类分级 / 敏感数据 / 脱敏 / 审计（哈希链） |
| **管理与空间** | 租户 / 组织 / 角色 / 消息 / 偏好 / 轻舟智助 |

---

## 4. 模块地图（Maven 模块结构）

Apache 风格平铺命名，Java 包名保持 `org.temporedata` 不变：

| 模块 | packaging | 职责 |
|---|---|---|
| `temporedata-bom` | pom | 全工程第三方依赖版本的**唯一出口**（import Spring Boot BOM + 覆盖平台版本） |
| `temporedata-common` | jar | 基础设施：工具、`CryptoUtil`、异常基类、`DistributedLockProvider` |
| `temporedata-api` | jar | 对外契约：DTO、`BaseResponse`、`PageRes`、数据源 SPI、`Dialect`/`DatasourceType`、安全策略 |
| `temporedata-support` | jar | 统一异常处理、缓存配置（Caffeine/Redis）、异步执行器 |
| `temporedata-security` | jar | 认证授权（JWT）/ Spring Security 规则 / 租户 / TraceId |
| `temporedata-service` | jar | 核心业务服务域（7 业务域，含数据源元数据 CRUD + `/api/driver` 插件管理） |
| `temporedata-datasource-plugin` | pom | 数据源插件**聚合父**（DolphinScheduler 风格，各库一模块） |
| `temporedata-datasource-mysql / -postgresql / -clickhouse / -oracle / -doris / -starrocks` | jar | 各数据源插件（连接串 / 方言 / JDBC 驱动） |
| `temporedata-server` | jar | 启动装配 / Flyway 迁移 / 前端静态 / 单 jar repackage |

> 顶层共 9 个模块（8 后端 + 1 前端）；数据源插件为聚合父内的子模块。依赖顺序约束（防循环）：`bom` 居首 → `common/api/support/security` → `service` → `datasource-plugin*` → `server`。

### 后端 7 业务域（`temporedata-service`）

| 域 | 包 | 职责 | controller 规模 |
|---|---|---|---|
| sys | `modules.sys` | 用户 / 角色 / 租户 / 组织 / 认证 / 菜单权限 / 消息 / 审计源 | 12 |
| dev | `modules.dev` | 工作流、SQL 查询、调度、发布审批、依赖 / 函数 / 全局变量、权限中心 | 14 |
| ops | `modules.ops` | 监控 / 集群 / 容器 / 引擎 / 告警 / 基线 / 审计、GitOps、数据接入、同步、实时 | 13 |
| gov | `modules.gov` | 元数据（meta）、血缘（lineage）、数据质量（quality）、数据访问控制与脱敏（security）、敏感数据、审计中心 | 10 |
| svc | `modules.svc` | DataApi 接口服务、接口日志、黑白名单、报表、表单 | 6 |
| asset | `modules.asset` | 数据中心、指标、标签、我的数据（MyData）、权限审批 | 5 |
| integration | `modules.integration` | 数据源（含驱动 / 插件 / 密钥 / 文件 / 数据安全掩码规则） | 5 |

### 前端结构（`temporedata-ui`）

- **栈**：Vue 3（`<script setup>`）+ Vite + Element Plus + Pinia + Vue Router + Axios + ECharts / AntV G6。
- **目录**：`views/`（59 功能页）、`api/modules/`（按域封装）、`stores/`（auth 等）、`config/menu.config.js`（信息架构）、`router/index.js`、`lib/permission.js`（`hasPerm` + 全局 `v-perm` 指令）。
- **构建**：`npm run build` 输出至 `temporedata-server` 静态目录；开发 `npm run dev`（端口 5174，`/api` 代理到 8080）。

---

## 5. 与其他文档的关系

| 侧重点 | 文档 |
|---|---|
| 分层架构、模块职责、进程与集成、横切能力 | [../design/architecture.md](../design/architecture.md) |
| 前端血缘模块的界面与交互设计 | [../ui/lineage.md](../ui/lineage.md) |
| 血缘模块运行期说明与真实浏览器走查 | 项目级 `../README.md` 与 [../zh/lineage.md](../zh/lineage.md) |