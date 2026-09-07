# 轻舟数据云 · Qingzhouyun

统一的数据开发、资产、服务与治理的一体化数据云平台。

`temporedata` 是一个前后端合一的单体交付的数据平台：`temporedata-ui`（Vue 3 SPA）+ `temporedata-server`（Spring Boot），最终以单个可执行 JAR 同源托管前端静态资源与后端 API。

## 快速开始

```bash
# 一键构建（前端 + 后端 → dist/temporedata.jar）
./tools/build.sh

# 本地容器化部署（MySQL + 应用）
docker compose -f docker/docker-compose.yml up -d --build
```

默认账号：`admin / admin123`（部署后请立即修改）。

## 核心能力

- 数据开发：工作流（DAG/条件分支/血缘）、SQL、实时计算、发布审批
- 数据资产：目录/元数据/指标/标签/质量/血缘
- 数据服务：API/报表/表单/日志/黑白名单
- 运行中心：调度/告警/基线/资源监控
- 安全治理：权限(RBAC)/分类分级/敏感数据/脱敏/审计（哈希链）
- 管理中心与个人空间：租户/组织/角色/消息/偏好/轻舟智助

## 目录结构

Apache 风格平铺命名（Java 包名保持 `org.temporedata` 不变）：

```text
temporedata-ui/     前端 SPA
temporedata-common/ 通用（BaseResponse/PageRes/异常）
temporedata-api/    对外契约 DTO + 数据源 SPI（org.temporedata.api.datasource）
temporedata-support/ 缓存/锁/异常/上下文/加密
temporedata-security/ 认证授权/JWT/租户/Trace
temporedata-service/ 业务服务域（含数据源元数据 CRUD + /api/driver 插件管理）
temporedata-datasource-plugin/ 数据源插件聚合父（各库一模块，DolphinScheduler 风格）
  ├── temporedata-datasource-mysql/  MySQL
  ├── temporedata-datasource-postgresql/  PostgreSQL
  ├── temporedata-datasource-clickhouse/  ClickHouse
  ├── temporedata-datasource-oracle/  Oracle
  ├── temporedata-datasource-doris/  Doris
  └── temporedata-datasource-starrocks/  StarRocks
temporedata-server/  启动装配/迁移/静态
docs/  docker/  tools/  dist/
```

## 文档

- [文档中心（docs/README.md）](docs/README.md) 总索引：产品 / 界面设计 / 代码技术设计 / 使用指南
- [docs/product](docs/product) 产品设计（定位 / 工作域 / 核心能力）
- [docs/ui](docs/ui) 界面设计（规范 / 血缘 UI）
- [docs/design](docs/design) 代码 / 技术设计（架构与各模块）
- [docs/zh](docs/zh) 使用与开发指南

## 数据血缘模块（Lineage）运行期说明

血缘分析位于 `/#/lineage`，新版为 `LineageV2.vue` 的 4 区块布局：左资产树 / 顶部 Toolbar（7 分组）/ 中央 AntV G6 画布 / 右侧三段详情面板 + 底部状态栏。路由分叉：`/#/lineage?layout=v2`（默认新版）与 `?layout=legacy`（旧版回退），选择记忆于 `localStorage.td_lineage_layout`。

### 运行前提

- 后端 `temporedata-server`（默认 8080）：MySQL + Flyway 自动建表迁移；需先有 `temporedata` 库及可登录账号（默认 `admin/admin123`）。
- 前端 dev：`cd temporedata-ui && npm run dev`（Vite，端口 5174，`/api` 代理到 8080）。
- 血缘图数据由 `POST /api/lineage/rebuild` 从 `temporedata_workflow_lineage`（任务级血缘）与 `temporedata_meta_table`（元数据表）生成。

### 真实浏览器走查（回归）

`scripts/lineage-walkthrough.cjs` 借助 Chrome 无头 + CDP（无 Playwright/Puppeteer 依赖，仅 Node 内置 WebSocket）自动验证：血缘页 V2 挂载、admin 写门控、根节点图渲染、搜索下拉隐藏 stats 覆盖层、无 console 报错。

```bash
node scripts/lineage-walkthrough.cjs --root tbl:dws_sales_daily --open-search --out /tmp
# --root        指定根节点以渲染图（省略则只验挂载与写门控）
# --open-search 额外校验“打开搜索下拉时隐藏画布 stats 覆盖层”
# --frontend/--backend 可覆盖地址；输出 WALK_OK/FAIL，exit code 0/1
# 前置：后端(8080) 与前端(5174) 需在运行；本机需 Chrome
```

## 贡献

请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 许可

[Apache License 2.0](LICENSE)。附带属性声明见 [NOTICE](NOTICE)。

## 安全

发现安全问题时，请遵循 [SECURITY.md](SECURITY.md) 负责任披露。