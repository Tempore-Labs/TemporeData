# v2.0 前端 · 壳 + 核心页迁移清单

> 原则（`03-frontend-code-architecture.md` / `22-code-refactor-sequence.md`）：先建壳，再迁核心 15–20 页，后逐页平移旧页；不一次性重写。
> 「迁移完成」= 每页满足迁移集（见 §3）。勾选即完成。

## 1. 壳（Shell）— 已建 ✅
已完成（`temporedata-ui/src/`，未接线到 main/router，不影响现有应用）：
- [x] `design-system/tokens.css`（token/主题变量）
- [x] `services/http.js`（`/api/v1` axios 封装：JWT、Idempotency-Key、ApiResponse、错误映射）
- [x] `composables/usePageState.js`（loading/empty/error/success/permission-denied/offline）
- [x] `permissions/index.js`（hasPerm / canExecute + 高风险动作清单）
- [x] `layouts/AppLayoutV2.vue` + `layouts/navigation.js`（新布局+导航模型）
- [x] `components/CommandPalette.vue`（Ctrl+K）
- [x] `components/base/{StatusBadge,MetricCard,DataTable}.vue`（骨架+状态机）
- [x] `features/{dashboard,jobs}`（示例：api/types/routes/views）
- [x] `app/routes.js` + `features/index.js`（v2 路由表，未挂载）+ `app/README.md`

**待接线（切导航时）**：把 `v2RootRoute` 挂进现有 `router`（单独一步，避免影响现有应用）。

## 2. 未建的其余基础/工具（补壳）
- [ ] `components/base/ResourceDrawer.vue`（详情抽屉）
- [ ] `services/{auth,realtime}.js`
- [ ] `app/{bootstrap,providers}.js`
- [x] 其余 7 个 feature 骨架（cluster/compute/monitoring/logging/incidents/automation/data）按 features/jobs 模板补齐
- [x] `features/{compute,logging,automation,data,admin}` 本批 8 页（api/types/routes/views）建成并并入 `features/index.js`

## 3. 迁移集（每页「完成」标准）
- [ ] 路由并入 v2 表，走 `features/*/routes.js` 懒加载
- [ ] `api.js` 调 `/api/v1`（经 `services/http`），**View 不直连 axios**
- [ ] `types.js` 语义类型（JS JSDoc）
- [ ] `permission gate`（`hasPerm`，denied→`permission-denied` 态）
- [ ] UI 状态机：loading/empty/error/success/permission-denied/offline
- [ ] 高风险动作进 Action Preview，不直接执行
- [ ] Playwright 用例

## 4. 核心 15–20 页迁移清单
> P=页面模块，C=集群(Cluster)，J=作业(Job)，M=监控，I=事件，D=数据，A=自动化。

| # | 页面（路由） | feature | migration 集 | 状态 |
|---|---|---|---|---|
| 1 | 总览 `/v2/dashboard` | dashboard | ①-⑦ | ✅ 示例 |
| 2 | 集群列表 `/v2/clusters` | cluster | ①-⑦ | ☐ |
| 3 | 集群详情 `/v2/clusters/:id` | cluster | ①-⑦ | ☐ |
| 4 | 计算 `/v2/compute` | compute | ①-⑦ | ✅ |
| 5 | 作业列表 `/v2/jobs` | jobs | ①-⑦ | ✅ 示例 |
| 6 | 作业详情 `/v2/jobs/:id` | jobs | ①-⑦ | ☐ |
| 7 | 作业执行 `/v2/jobs/:id/executions/:executionId` | jobs | ①-⑦ | ☐ |
| 8 | 监控 `/v2/monitoring` | monitoring | ①-⑦ | ☐ |
| 9 | 日志 `/v2/logs` | logging | ①-⑦ | ✅ |
| 10 | 事件列表 `/v2/incidents` | incidents | ①-⑦ | ☐ |
| 11 | 事件详情 `/v2/incidents/:id` | incidents | ①-⑦ | ☐ |
| 12 | 自动化 Runbook `/v2/automation/runbooks` | automation | ①-⑦ | ✅ |
| 13 | 自动化执行 `/v2/automation/executions` | automation | ①-⑦ | ✅ |
| 14 | 数据源 `/v2/data/datasources` | data | ①-⑦ | ✅ |
| 15 | 元数据 `/v2/data/metadata` | data | ①-⑦ | ✅ |
| 16 | 血缘 `/v2/data/lineage` | data | ①-⑦ | ✅ |
| 17 | 管理 `/v2/admin` | admin | ①-⑦ | ✅ |

> 迁移顺序建议：先 1,5（已完成示例）→ 2,3 → 8 → 4 → 16（血缘复用现有 LineageGraph）→ 14,15 → 9 → 10,11 → 12,13 → 6,7 → 17（每页独立 Playwright）。

## 5. 切换默认导航 + 弃旧（P7）
- [ ] 把 `v2RootRoute` 挂进 router，`/v2/**` 可用
- [ ] 新 Layout/CommandPalette 设为默认导航（`/` 重定向到 `/v2/dashboard` 或保留旧入口递进切换）
- [ ] 旧页平移进 feature 结构（不重写），逐步关闭旧路由
- [ ] **弃旧硬门禁**：v1 API 100% 覆盖 / 前端不再调 legacy / 数据迁移完成 / 审计可追溯 / E2E 过 / 两版本周期无回滚