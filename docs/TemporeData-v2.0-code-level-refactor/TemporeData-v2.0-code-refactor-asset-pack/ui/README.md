# UI 设计（Interface Design）

本目录收录**前端界面设计**相关内容：通用 UI 设计原则 / 规范要点，以及数据血缘分析模块的界面与交互设计方案。

## 文档列表

| 文档 | 内容 | 定位 |
|---|---|---|
| [design-principles.md](design-principles.md) | 通用 UI 设计原则与规范要点：设计目标、Fitts 定律 / 渐进披露 / 双轨反馈 / 无障碍先行、设计 Token（色板 / 间距 / 圆角 / 主题变量）、Element Plus 覆盖、组件规范表头约定 | 前端通用规范基线 |
| [lineage.md](lineage.md) | 血缘模块 UI 设计：4 区块布局、Toolbar 7 分组、G6 画布、三段式详情面板、3 写对话框统一 FormSchema、6 快捷键、i18n/a11y/主题、路由分叉、真实浏览器走查 | 血缘模块界面专项设计 |

## 适用范围与基线

- **技术栈基线**：Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios + AntV G6，浅色主题。
- **血缘模块运行期**：血缘分析位于 `/#/lineage`，新版为 `LineageV2.vue` 的 4 区块布局，路由分叉 `?layout=v2`（默认新版）与 `?layout=legacy`（旧版回退），选择记忆于 `localStorage.td_lineage_layout`。详见 [lineage.md](lineage.md) 与 [../zh/lineage.md](../zh/lineage.md)。
- 产品定位与模块地图见 [../product/overview.md](../product/overview.md)。
- 血缘模块后端设计与 `/api/lineage` 数据契约见 [../design/module-lineage.md](../design/module-lineage.md)。