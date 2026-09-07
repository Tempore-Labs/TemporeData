# v2.0 前端壳（Shell）— 使用与接线说明

本目录是 TemporeData v2.0 前端「壳骨架」：**只新增、未接线到 `main.js` / 现有路由**，因此不影响现有应用与构建。

## 已包含
- `design-system/tokens.css` —— 设计 token/主题变量
- `services/http.js` —— `/api/v1` axios 封装（JWT、`Idempotency-Key`、`ApiResponse{code,msg,data,requestId,traceId}`、错误映射）
- `composables/usePageState.js` —— 页面状态机（loading/empty/error/success/permission-denied/offline）
- `permissions/index.js` —— `hasPerm`/`canExecute` + 高风险动作清单
- `layouts/AppLayoutV2.vue` + `layouts/navigation.js` —— 新主布局与导航模型
- `components/CommandPalette.vue` —— Ctrl+K 命令面板
- `components/base/{StatusBadge,MetricCard,DataTable}.vue` —— 基础组件骨架
- `features/{dashboard,jobs}` —— Feature 模块示例（`api.js/types.js/routes.js/views`）
- `app/routes.js` —— v2 路由表（未挂载）

## 目前未做（待后续步骤）
- 未把 `v2RootRoute` 挂进现有 `router`（避免影响现有应用）——接线需单独一步。
- 仅 2 个 feature 示例；其余 7 个 feature（cluster/compute/monitoring/logging/incidents/automation/data）待按模板补齐。
- 页面为占位骨架，未联真实 `/api/v1` 业务接口（部分为占位端）。

## 接线方式（迁移切导航时）
```js
// 将来在 main.js / router 入口：
// router.addRoute(v2RootRoute)
```