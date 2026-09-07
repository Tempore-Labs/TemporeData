import { NAV } from '../../layouts/navigation'

export const routes = [
  { path: NAV.automation.path, name: 'V2.Automation.Runbooks', component: () => import('./views/Runbooks.vue'), meta: { title: '自动化 Runbook', perm: 'automation:read' } },
  { path: '/v2/automation/executions', name: 'V2.Automation.Executions', component: () => import('./views/Executions.vue'), meta: { title: '自动化执行', perm: 'automation:read' } }
]