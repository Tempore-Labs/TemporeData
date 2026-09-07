# 数据血缘模块开发与使用指南

> 对应前端路由 `/#/lineage`；后端接口前缀 `/api/lineage`。

## 1. 页面结构（V2 新版）

4 区块布局（`temporedata-ui/src/views/LineageV2.vue`）：

| 区块 | 说明 |
|---|---|
| `[B]` 资产树 `LgAssetTree` | 🌳 资产树 / 🕒 最近 / 📑 书签 三 Tab；由当前图数据按 domain→owner→name 分组 |
| `[A]` Toolbar | 7 分组：加载起点 · 图级别 · 多维过滤 · Dim/Hide · 方向/深度/类型 · 图层+布局+环检测 · 动作(路径/热度/SQL/血缘/导出) |
| `[C]` 中央画布 `LineageGraph` | AntV G6 只读图：空态/加载骨架/图；Minimap、Legend、节点拖拽、边 Popover、右键上下文菜单 |
| `[D]` 详情面板 | §1 资产元信息(可展开治理/FQN复制/DQ徽章) · §2 字段列表(Tab+搜索+列高亮) · §3 关联节点(方向Tab+删除+添加关联) + 动作条 |
| 状态栏 `footer` | 连接状态 · 节点/边 · 渲染耗时 · 权限 chip |

路由分叉：`LineageEntry.vue` 按 `?layout=v2|legacy` 选择，默认 v2，记忆于 `localStorage.td_lineage_layout`；新旧视图按需分包。

## 2. 前端模块与组件

- 视图：`views/LineageV2.vue`（新版）、`views/Lineage.vue`（旧版回退）、`views/LineageEntry.vue`（分叉）。
- 组件（`src/components/lineage/`）：
  - `LineageGraph.vue`（G6 画布，复用，状态/结构双轨更新）
  - `LgAssetTree / LgEmptyState / LgLoadingSkeleton`
  - `LgDqBadge / LgCopyFqn / LgColumnList / LgStatusTab / LgNeighborCard / LgDialogActionBar / LgEdgeFormFields`
- 组合式函数（`src/composables/`）：`useLineageEdgeFormSchema.js`（3 写对话框共享校验）、`useLineageKeyboard.js`（6 快捷键）。
- 状态：`src/stores/lineage.js`（图数据/图层/布局覆盖/列高亮/DQ/书签/最近浏览/渲染统计）。
- 接口：`src/api/modules/lineage.js`（graph/overview/search/heat/path/cycles/dataquality/edge PUT·PATCH·DELETE 等）。

## 3. 数据契约（后端 `/api/lineage`）

- 图数据：`GET /graph?nodeId=`, 从 `temporedata_lineage`（由 `POST /rebuild` 生成）读取。
- 血缘图来源：`POST /rebuild` 从 `temporedata_workflow_lineage`（任务级 READ/WRITE 边）+ `temporedata_meta_table`（孤立表节点）构建。
- 写操作：`PUT/PATCH/DELETE /edge/{from}/{to}`，受 `LINEAGE:WRITE` 权限（admin 绕过）。
- 详情/探索：`GET /{entityType}/{id}`、`/name/{fqn}`、`/explore`、`/hydrate`、`/dataquality`。

## 4. 权限

- 超级管理员：角色含 `ROLE_ADMIN`（后端 `LineageAuthHelper`/`AuthService` 精确匹配）恒有权限。
- 可写用户：在 `res_permission` 授予 `resource_type='LINEAGE'`、`resource_key='*'`、`action='WRITE'`。
- 前端写入口统一 `hasPerm('LINEAGE:WRITE')` 判定；`isAdmin` 不再按 `admin` 子串误判租户管理员。

## 5. 运行

```bash
# 后端（8080，MySQL + Flyway 自动建表）
cd temporedata-server && DB_USERNAME=root DB_PASSWORD=... mvn spring-boot:run
# 前端（5174，/api 代理到 8080）
cd temporedata-ui && npm run dev
# 生成血缘图（需先有 workflow_lineage / meta_table 数据）
# 本机/CI 可用脚本种子：mysql ... temporedata < scripts/demo-lineage-seed.sql
curl -X POST http://localhost:8080/api/lineage/rebuild -H "Authorization: Bearer <token>"
```

## 6. 扩展

- 加新过滤/图层：更新 `stores/lineage.js` 状态 + `LineageV2` Toolbar 组 + `LineageGraph` props/state。
- 加写对话框：在 `useLineageEdgeFormSchema.js` 的 `edgeFormMeta` 增字段，三个对话框自动同步。
- 加回归断言：扩展 `scripts/lineage-walkthrough.cjs` 的 `check()` 步骤。

## 7. CI 回归（可选）

详见 `.github/workflows/lineage-walkthrough.yml`（`workflow_dispatch` 手动触发）。