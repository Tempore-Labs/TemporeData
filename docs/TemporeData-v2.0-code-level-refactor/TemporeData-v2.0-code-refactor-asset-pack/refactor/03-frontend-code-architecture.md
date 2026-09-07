# 03 Vue 前端代码级架构

## 1. 目标目录

```text
temporedata-ui/src
├── app/
│   ├── router/
│   ├── providers/
│   └── bootstrap/
├── layouts/
├── components/
│   ├── DataTable/
│   ├── MetricCard/
│   ├── StatusBadge/
│   ├── ResourceDrawer/
│   └── CommandPalette/
├── features/
│   ├── dashboard/
│   ├── cluster/
│   ├── compute/
│   ├── jobs/
│   ├── monitoring/
│   ├── logging/
│   ├── incidents/
│   ├── automation/
│   └── data/
├── stores/
├── services/
│   ├── http/
│   ├── auth/
│   └── realtime/
├── composables/
├── permissions/
├── design-system/
└── utils/
```

## 2. Feature 模块模板

```text
features/jobs/
├── api.ts
├── types.ts
├── constants.ts
├── routes.ts
├── stores/
│   └── job.store.ts
├── components/
│   ├── JobTable.vue
│   ├── JobStatus.vue
│   └── JobExecutionDrawer.vue
└── views/
    ├── JobList.vue
    ├── JobDetail.vue
    └── JobExecution.vue
```

## 3. API 层

禁止 View 直接 Axios：

```ts
// api.ts
export const listJobs = (params: JobQuery) =>
  http.get<PageResponse<Job>>('/api/v1/jobs', { params })
```

View 只调用：

```ts
const { data } = await listJobs(query)
```

## 4. 状态管理

Pinia 只管理跨页面状态：

- auth
- tenant
- global preferences
- realtime connection
- notification
- command palette

页面筛选、分页、drawer 状态优先使用组件本地状态。

## 5. UI 状态机

统一：

```text
loading
empty
error
success
permission-denied
offline
```

资源状态：

```text
healthy
running
warning
critical
stopped
unknown
maintenance
```

## 6. 页面设计

核心页面：

```text
/dashboard
/clusters
/clusters/:id
/compute
/jobs
/jobs/:id
/jobs/:id/executions/:executionId
/monitoring
/logs
/incidents
/incidents/:id
/automation/runbooks
/automation/executions
/data/datasources
/data/metadata
/data/lineage
/admin
```

资源详情优先 Drawer / Side Panel，减少页面跳转。

## 7. 全局 Command Palette

`Ctrl+K`：

```text
搜索资源
搜索 Job
搜索 Cluster
跳转页面
执行安全 Runbook
查看 Incident
```

高风险动作不允许从 Command Palette 直接执行，必须进入 Action Preview。
