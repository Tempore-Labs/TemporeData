import { createRouter, createWebHashHistory } from 'vue-router'
import { v2RootRoute } from '@/app/routes'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { noAuth: true }
  },
  v2RootRoute,
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      // 资源管理
      { path: 'cluster', name: 'Cluster', component: () => import('@/views/Cluster.vue'), meta: { title: '计算集群' } },
      { path: 'datasource', name: 'Datasource', component: () => import('@/views/Datasource.vue'), meta: { title: '数据源' } },
      { path: 'container', name: 'Container', component: () => import('@/views/Container.vue'), meta: { title: '计算容器' } },
      { path: 'resource', name: 'ResourceCenter', component: () => import('@/views/ResourceCenter.vue'), meta: { title: '资源中心' } },
      // 数据研发
      { path: 'workflow', name: 'Workflow', component: () => import('@/views/Workflow.vue'), meta: { title: '数据开发' } },
      { path: 'realtime', name: 'Realtime', component: () => import('@/views/Realtime.vue'), meta: { title: '实时计算' } },
      { path: 'globalvar', name: 'GlobalVar', component: () => import('@/views/GlobalVar.vue'), meta: { title: '全局变量' } },
      { path: 'funcrepo', name: 'FuncRepo', component: () => import('@/views/FuncRepo.vue'), meta: { title: '函数仓库' } },
      { path: 'dependency', name: 'Dependency', component: () => import('@/views/Dependency.vue'), meta: { title: '依赖合集' } },
      { path: 'approval', name: 'Approval', component: () => import('@/views/Approval.vue'), meta: { title: '发布审批' } },
      // 数据运维
      { path: 'monitor', name: 'Monitor', component: () => import('@/views/Monitor.vue'), meta: { title: '资源监控' } },
      { path: 'scheduler', name: 'Scheduler', component: () => import('@/views/Scheduler.vue'), meta: { title: '调度中心' } },
      { path: 'calendar', name: 'Calendar', component: () => import('@/views/Calendar.vue'), meta: { title: '自定义日历' } },
      { path: 'alarm', name: 'Alarm', component: () => import('@/views/Alarm.vue'), meta: { title: '告警配置' } },
      { path: 'baseline', name: 'Baseline', component: () => import('@/views/Baseline.vue'), meta: { title: '基线告警' } },
      { path: 'ops', name: 'Ops', component: () => import('@/views/Ops.vue'), meta: { title: 'Ops 集成' } },
      // 数据监控
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '监控总览' } },
      { path: 'meta', name: 'Meta', component: () => import('@/views/Meta.vue'), meta: { title: '结构采集' } },
      { path: 'datacenter', name: 'DataCenter', component: () => import('@/views/DataCenter.vue'), meta: { title: '数据中心' } },
      // 数据治理
      { path: 'quality', redirect: '/qualityrule' },
      { path: 'qualityrule', name: 'QualityRule', component: () => import('@/views/Quality.vue'), meta: { title: '质量规则' } },
      // 数据安全
      { path: 'auditlog', name: 'AuditLog', component: () => import('@/views/AuditLog.vue'), meta: { title: '审计总览' } },
      { path: 'changeaudit', name: 'ChangeAudit', component: () => import('@/views/ChangeAudit.vue'), meta: { title: '变更审计' } },
      { path: 'datalevel', name: 'DataLevel', component: () => import('@/views/DataLevel.vue'), meta: { title: '分类分级' } },
      { path: 'sensitive', name: 'Sensitive', component: () => import('@/views/Sensitive.vue'), meta: { title: '敏感数据' } },
      { path: 'mydata', name: 'MyData', component: () => import('@/views/MyData.vue'), meta: { title: '我的数据' } },
      { path: 'permapproval', name: 'PermApproval', component: () => import('@/views/PermApproval.vue'), meta: { title: '权限审批' } },
      // 数据资产
      { path: 'indicator', name: 'Indicator', component: () => import('@/views/Indicator.vue'), meta: { title: '数据指标' } },
      { path: 'tag', name: 'Tag', component: () => import('@/views/Tag.vue'), meta: { title: '数据标签' } },
      { path: 'dataasset', name: 'DataAsset', component: () => import('@/views/DataAsset.vue'), meta: { title: '数据目录' } },
      { path: 'lineage', name: 'Lineage', component: () => import('@/views/LineageEntry.vue'), meta: { title: '血缘分析' } },
      // 数据服务
      { path: 'datamask', name: 'DataMask', component: () => import('@/views/DataMask.vue'), meta: { title: '数据大屏' } },
      { path: 'dataapi', name: 'DataApi', component: () => import('@/views/DataApi.vue'), meta: { title: '接口服务' } },
      { path: 'apilog', name: 'ApiLog', component: () => import('@/views/ApiLog.vue'), meta: { title: '接口日志' } },
      { path: 'blacklist', name: 'Blacklist', component: () => import('@/views/Blacklist.vue'), meta: { title: '黑白名单' } },
      { path: 'report', name: 'Report', component: () => import('@/views/Report.vue'), meta: { title: '数据报表' } },
      { path: 'form', name: 'Form', component: () => import('@/views/Form.vue'), meta: { title: '表单管理' } },
      // 后台管理
      { path: 'tenant', name: 'Tenant', component: () => import('@/views/Tenant.vue'), meta: { title: '租户成员' } },
      { path: 'role', name: 'Role', component: () => import('@/views/Role.vue'), meta: { title: '角色管理' } },
      { path: 'org', name: 'Org', component: () => import('@/views/Org.vue'), meta: { title: '组织架构' } },
      { path: 'notify', name: 'Notify', component: () => import('@/views/Notify.vue'), meta: { title: '通知配置' } },
      { path: 'agent', name: 'Agent', component: () => import('@/views/Agent.vue'), meta: { title: 'Agent助手' } },
      { path: 'settings', name: 'Settings', component: () => import('@/views/Settings.vue'), meta: { title: '后台设置' } },
      // 平台管理
      { path: 'passwordless', name: 'Passwordless', component: () => import('@/views/Passwordless.vue'), meta: { title: '免密登录' } },
      { path: 'perm', name: 'PermissionCenter', component: () => import('@/views/PermissionCenter.vue'), meta: { title: '权限中心' } },
      { path: 'audit-center', name: 'AuditCenter', component: () => import('@/views/AuditCenter.vue'), meta: { title: '审计中心' } },
      { path: 'security-gov', name: 'SecurityGov', component: () => import('@/views/SecurityGov.vue'), meta: { title: '安全治理' } },
      // 个人中心
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { title: '个人中心' } },
      { path: 'message', name: 'Message', component: () => import('@/views/Message.vue'), meta: { title: '消息中心' } },
      // 遗留页面
      { path: 'synctask', name: 'SyncTask', component: () => import('@/views/SyncTask.vue'), meta: { title: '数据同步' } },
      { path: 'ingestion', name: 'Ingestion', component: () => import('@/views/Ingestion.vue'), meta: { title: '数据采集' } },
      { path: 'engine', name: 'Engine', component: () => import('@/views/Engine.vue'), meta: { title: '引擎作业' } },
      { path: 'secret', name: 'Secret', component: () => import('@/views/Secret.vue'), meta: { title: '密钥管理' } },
      { path: 'ha', name: 'Ha', component: () => import('@/views/Ha.vue'), meta: { title: '高可用' } },
      { path: 'work', name: 'Work', component: () => import('@/views/Work.vue'), meta: { title: '作业管理' } },
      { path: 'viewpage', name: 'ViewPage', component: () => import('@/views/ViewPage.vue'), meta: { title: '视图管理' } },
      { path: 'sqleditor', name: 'SqlEditor', component: () => import('@/views/SqlEditor.vue'), meta: { title: 'SQL 编辑器' } }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// Auth guard
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('td_token')
  if (to.meta.noAuth) {
    if (token && to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    if (!token) {
      next('/login')
    } else {
      next()
    }
  }
})

export default router