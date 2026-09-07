# 轻舟数据云（TemporeData）文档中心

统一的数据开发、资产、服务与治理一体化数据云平台 —— 文档总索引。

本文档中心按 **产品 / 界面 / 代码设计 / 使用指南** 四个维度分层组织，覆盖从产品定位、信息架构，到后端技术设计、前端 UI 规范，再到血缘模块开发与使用指南的完整链路。文档之间通过相对链接相互导航，每篇均自包含、可单独阅读。

---

## 📍 快速入口

- [项目 README（`../README.md`）](../README.md)：项目简介、快速开始（一键构建 / Docker 部署）、核心能力与目录结构。
- [产品总览（overview）](product/overview.md)：产品定位、七大工作域、核心能力清单、模块地图。
- [设计方案索引（design）](design/README.md)：后端架构与 12 篇技术设计的归并总览 + 模块关系图。

> 目录四大部分：**[Product（产品）](#-product--产品)** · **[UI（界面设计）](#-ui--界面设计)** · **[Design（代码/技术设计）](#-design--代码技术设计)** · **[Guide（使用指南）](#-guide--使用指南)**

---

## 📦 Product · 产品

产品定位、业务能力划分与平台宏观结构。

| 文档 | 说明 |
|---|---|
| [product/README.md](product/README.md) | 产品文档索引 |
| [product/overview.md](product/overview.md) | 产品定位、「数据研发 / 数据资产 / 数据服务 / 运行中心 / 安全治理 / 管理中心 / 个人空间」7 大工作域、核心能力清单、后端 7 业务域与前端信息架构的模块地图 |

---

## 🎨 UI · 界面设计

血缘分析模块（前后端）的界面结构与功能重设计方案与通用设计规范。

| 文档 | 说明 |
|---|---|
| [ui/README.md](ui/README.md) | UI 设计索引 |
| [ui/design-principles.md](ui/design-principles.md) | 通用 UI 设计原则与规范要点：设计目标、Fitts 定律 / 渐进披露 / 双轨反馈 / 无障碍先行、设计 Token（色板 / 间距 / 圆角 / 主题变量）、Element Plus 主题覆盖、组件规范表头约定 |
| [ui/lineage.md](ui/lineage.md) | 血缘模块 UI 设计：4 区块布局、Toolbar 7 分组、G6 画布（Minimap / 图例 / 截图锁）、三段式详情面板、3 个写对话框统一 FormSchema、6 条快捷键、i18n / a11y / 主题、路由分叉 `v2\|legacy`、真实浏览器走查脚本 |

---

## ⚙️ Design · 代码 / 技术设计

后端架构、模块划分与各子系统详细技术设计（由原 `docs/design/temporedata-*.md` 十二篇归并而成）。

| 文档 | 说明 |
|---|---|
| [design/README.md](design/README.md) | 设计索引 + 模块关系图（文本） |
| [design/architecture.md](design/architecture.md) | 架构与功能设计：分层 / 模块 / 进程 / 集成 |
| [design/module-datasource.md](design/module-datasource.md) | 数据源插件化（v2.x）+ 连接池方案与多数据源 SQL 血缘解析 |
| [design/module-sql-parser.md](design/module-sql-parser.md) | SQL 解析器设计与 Druid 定位 + 单一 SQL 解析器 Druid 化重构 |
| [design/module-metadata.md](design/module-metadata.md) | 元数据管理模块自动化设计 |
| [design/module-lineage.md](design/module-lineage.md) | 数据血缘模块（后端）重新设计 + 连接池 SQL 血缘解析（血缘部分） |
| [design/module-security.md](design/module-security.md) | 数据访问控制与动态脱敏执行链设计 |
| [design/infra-bom.md](design/infra-bom.md) | BOM 统一依赖管理设计 |
| [design/runtime-jar.md](design/runtime-jar.md) | 运行模式重设计：开发 / 迭代免重复打 jar |
| [design/asset-inventory.md](design/asset-inventory.md) | 项目资产清单（非数据库表部分） |

---

## 📖 Guide · 使用指南

模块的开发与使用指南。

| 文档 | 说明 |
|---|---|
| [zh/lineage.md](zh/lineage.md) | 数据血缘开发与使用指南：页面结构、前端模块、后端 `/api/lineage` 数据契约、权限、运行、扩展与 CI 回归 |

---

## 维护约定

- 本目录采用 **Product / UI / Design / Guide** 四层结构，各层均有 `README.md` 作为该层索引。
- 每一篇文档**自包含**（可脱离其他文档单独阅读），同时又通过相对链接互相引用，形成完整导航。
- 新增设计文档时，应按归属放入对应层目录，并同步更新对应层 `README.md` 与本文档的总索引。