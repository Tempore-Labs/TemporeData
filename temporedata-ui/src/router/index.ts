/**
 * Vue Router (hash mode) for TemporeData UI v3.
 * `/login` sits outside the AppShell; all other pages are AppShell children.
 */
import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/api/http'

const AppShell = () => import('@/layouts/AppShell.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { noAuth: true, title: '登录' },
  },
  {
    path: '/',
    component: AppShell,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据概览' },
      },
      {
        path: 'datasource',
        name: 'Datasource',
        component: () => import('@/views/datasource/index.vue'),
        meta: { title: '数据源管理' },
      },
      {
        path: 'catalog',
        name: 'Catalog',
        component: () => import('@/views/catalog/index.vue'),
        meta: { title: '数据目录' },
      },
      {
        path: 'lineage',
        name: 'Lineage',
        component: () => import('@/views/lineage/index.vue'),
        meta: { title: '数据血缘' },
      },
      {
        path: 'sql-editor',
        name: 'SqlEditor',
        component: () => import('@/views/sql-editor/index.vue'),
        meta: { title: 'SQL 编辑器' },
      },
      {
        path: 'workflow',
        name: 'Workflow',
        component: () => import('@/views/workflow/workspace.vue'),
        meta: { title: '工作流管理' },
      },
      {
        path: 'workflow/editor/:id',
        name: 'WorkflowEditor',
        component: () => import('@/views/workflow/editor.vue'),
        meta: { title: '工作流编辑器' },
      },
      {
        path: 'scheduler',
        name: 'Scheduler',
        component: () => import('@/views/scheduler/index.vue'),
        meta: { title: '调度中心' },
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/index.vue'),
        meta: { title: '监控总览' },
      },
      {
        path: 'quality',
        name: 'Quality',
        component: () => import('@/views/quality/index.vue'),
        meta: { title: '质量管理' },
      },
      {
        path: 'alarm',
        name: 'Alarm',
        component: () => import('@/views/alarm/index.vue'),
        meta: { title: '告警配置' },
      },
      {
        path: 'audit',
        name: 'Audit',
        component: () => import('@/views/audit/index.vue'),
        meta: { title: '审计中心' },
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('@/views/system/index.vue'),
        meta: { title: '系统管理' },
      },
      {
        path: 'perm',
        name: 'Perm',
        component: () => import('@/views/perm/index.vue'),
        meta: { title: '权限中心' },
      },
      {
        path: 'sensitive',
        name: 'Sensitive',
        component: () => import('@/views/sensitive/index.vue'),
        meta: { title: '敏感数据治理' },
      },
      {
        path: 'cluster',
        name: 'Cluster',
        component: () => import('@/views/cluster/index.vue'),
        meta: { title: '计算集群' },
      },
      {
        path: 'container',
        name: 'Container',
        component: () => import('@/views/container/index.vue'),
        meta: { title: '计算容器' },
      },
      {
        path: 'resource',
        name: 'Resource',
        component: () => import('@/views/resource/index.vue'),
        meta: { title: '资源中心' },
      },
      {
        path: 'tag',
        name: 'Tag',
        component: () => import('@/views/tag/index.vue'),
        meta: { title: '标签与指标' },
      },
      {
        path: 'security',
        name: 'Security',
        component: () => import('@/views/security/index.vue'),
        meta: { title: '安全治理' },
      },
      {
        path: 'data-service',
        name: 'DataService',
        component: () => import('@/views/data-service/index.vue'),
        meta: { title: '接口服务' },
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/report/index.vue'),
        meta: { title: '数据报表' },
      },
      {
        path: 'gateway',
        name: 'Gateway',
        component: () => import('@/views/gateway/index.vue'),
        meta: { title: '网关控制台' },
      },
      {
        path: 'blacklist',
        name: 'Blacklist',
        component: () => import('@/views/blacklist/index.vue'),
        meta: { title: '黑白名单' },
      },
      {
        path: 'ha',
        name: 'Ha',
        component: () => import('@/views/ha/index.vue'),
        meta: { title: '高可用' },
      },
      {
        path: 'datasets',
        name: 'Datasets',
        component: () => import('@/views/datasets/index.vue'),
        meta: { title: '数据集管理' },
      },
      {
        path: 'asset',
        name: 'Asset',
        component: () => import('@/views/asset/index.vue'),
        meta: { title: '资产指标' },
      },
      {
        path: 'contract',
        name: 'Contract',
        component: () => import('@/views/contract/index.vue'),
        meta: { title: '数据契约' },
      },
      {
        path: 'cost',
        name: 'Cost',
        component: () => import('@/views/cost/index.vue'),
        meta: { title: '成本治理' },
      },
      {
        path: 'incidents',
        name: 'Incidents',
        component: () => import('@/views/incidents/index.vue'),
        meta: { title: '事件闭环' },
      },
      {
        path: 'ai-safety',
        name: 'AiSafety',
        component: () => import('@/views/ai-safety/index.vue'),
        meta: { title: 'AI 门控' },
      },
      {
        path: 'rca',
        name: 'Rca',
        component: () => import('@/views/rca/index.vue'),
        meta: { title: 'RCA-Agent' },
      },
      {
        path: 'ingestion',
        name: 'Ingestion',
        component: () => import('@/views/ingestion/index.vue'),
        meta: { title: '数据接入' },
      },
      {
        path: 'sync',
        name: 'Sync',
        component: () => import('@/views/sync/index.vue'),
        meta: { title: '数据同步' },
      },
      {
        path: 'engine',
        name: 'Engine',
        component: () => import('@/views/engine/index.vue'),
        meta: { title: '计算引擎' },
      },
      {
        path: 'ops-git',
        name: 'OpsGit',
        component: () => import('@/views/ops-git/index.vue'),
        meta: { title: 'Git 运维' },
      },
      {
        path: 'mydata',
        name: 'Mydata',
        component: () => import('@/views/mydata/index.vue'),
        meta: { title: '我的数据' },
      },
      {
        path: 'datacenter',
        name: 'Datacenter',
        component: () => import('@/views/datacenter/index.vue'),
        meta: { title: '数据中心' },
      },
      {
        path: 'permapproval',
        name: 'Permapproval',
        component: () => import('@/views/permapproval/index.vue'),
        meta: { title: '权限审批' },
      },
      {
        path: 'apilog',
        name: 'Apilog',
        component: () => import('@/views/apilog/index.vue'),
        meta: { title: 'API 日志' },
      },
      {
        path: 'form',
        name: 'Form',
        component: () => import('@/views/form/index.vue'),
        meta: { title: '动态表单' },
      },
      {
        path: 'func',
        name: 'Func',
        component: () => import('@/views/func/index.vue'),
        meta: { title: '函数管理' },
      },
      {
        path: 'globalvar',
        name: 'Globalvar',
        component: () => import('@/views/globalvar/index.vue'),
        meta: { title: '全局变量' },
      },
      {
        path: 'dependency',
        name: 'Dependency',
        component: () => import('@/views/dependency/index.vue'),
        meta: { title: '依赖管理' },
      },
      {
        path: 'work',
        name: 'Work',
        component: () => import('@/views/work/index.vue'),
        meta: { title: '作业管理' },
      },
      {
        path: 'realtime',
        name: 'Realtime',
        component: () => import('@/views/realtime/index.vue'),
        meta: { title: '实时任务' },
      },
      {
        path: 'approval',
        name: 'Approval',
        component: () => import('@/views/approval/index.vue'),
        meta: { title: '审批中心' },
      },
      {
        path: 'file-center',
        name: 'FileCenter',
        component: () => import('@/views/file-center/index.vue'),
        meta: { title: '文件中心' },
      },
      {
        path: 'meta',
        name: 'Meta',
        component: () => import('@/views/meta/index.vue'),
        meta: { title: '元数据采集' },
      },
      {
        path: 'preference',
        name: 'Preference',
        component: () => import('@/views/preference/index.vue'),
        meta: { title: '系统偏好' },
      },
      {
        path: 'view',
        name: 'View',
        component: () => import('@/views/view/index.vue'),
        meta: { title: '视图管理' },
      },
      {
        path: 'passwordless',
        name: 'Passwordless',
        component: () => import('@/views/passwordless/index.vue'),
        meta: { title: '免密登录' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to) => {
  const token = getToken()
  if (to.meta.noAuth) {
    if (token && to.path === '/login') return '/dashboard'
    return true
  }
  if (!token) return '/login'
  document.title = `${(to.meta.title as string) || ''} · TemporeData`
  return true
})

export default router
