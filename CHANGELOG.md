# 更新日志 (Changelog)

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/) 与
[语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### 重构
- 参照 Apache 项目对仓库命名与结构进行开源化设计（文档见 `docs/design/Apache风格命名与结构重构设计_v1.0.md`）。
- Java 包名保持 `org.temporedata` 不变。

### 新增
- 开源治理文件：LICENSE（Apache-2.0）、NOTICE、README、CONTRIBUTING、CHANGELOG、SECURITY。

### 前端
- v2.0 信息架构收敛：7 工作域 + 管理中心 + 个人空间；侧边栏 208⇄64px 可折叠。
- Secret 密钥管理页对齐后端契约并做展示脱敏。
- 数据血缘 V2（`LineageV2.vue`）：4 区块布局（资产树/Toolbar 7 分组/G6 画布/三段详情+状态栏），多入口 `LineageEntry` 按 `?layout=v2|legacy` 分包切换；Phase B 详情三段式、3 写对话框 FormSchema 归一、6 快捷键、i18n/无障碍/主题变量、搜索下拉隐藏 stats 覆盖层。
- 登录 UserInfo 超级管理员判定对齐 `ROLE_ADMIN`（`user.admin` 权威下发）；前端超管判定收紧（不再按 `admin` 子串误判租户管理员）。

### 后端
- 异常契约统一（400/404/500）、PageRes 分页对象。
- Redis 分布式锁 + Redis 缓存（JSON+JavaTime 序列化）。
- RequestId/TraceId 可观测性（`X-Request-Id`、日志 MDC）。
- Entity→DTO 收敛：User（不再泄漏密码）、Secret、Datasource。
- 血缘修复：Flyway V20 全新引导迁移引用修正；`LineageAuthHelper`/`AuthService` 超级管理员判定对齐 `ROLE_ADMIN`；`saveEdge/patchEdge/deleteEdge` 补 `@CacheEvict` 避免写后图缓存陈旧。

### 脚本/工具
- `scripts/lineage-walkthrough.cjs`：血缘页真实浏览器回归（Chrome 无头 + CDP，无第三方依赖），自检后端/登录/前端并断言挂载、admin 写门控、图渲染、无 console 报错，退出码 0/1。

### CI/配置
- datasource JDBC URL 增 `allowPublicKeyRetrieval=true`：解决 MySQL 8 默认 `caching_sha2_password` + 非 SSL 下 HikariCP「Public Key Retrieval is not allowed」；CI 与本地全新库均可直连，无需 `ALTER USER ... mysql_native_password`。

### 文档
- 文档重组：整理为四层 docs（`product` 产品 / `ui` 界面设计 / `design` 代码技术设计 / `zh` 使用指南），`docs/README.md` 为总索引；归并 12 篇旧零散设计 md、删除旧 UI/血缘设计 md，项目级文档与 `docs/zh/lineage.md` 保留。

## [1.0.0] - 2026-08

### Added
- 初始版本：数据研发、数据资产、数据服务、运行中心、安全治理、管理中心、消息与轻舟智助。