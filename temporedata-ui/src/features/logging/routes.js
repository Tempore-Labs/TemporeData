import { NAV } from '../../layouts/navigation'

export const routes = [
  { path: NAV.logs.path, name: 'V2.Logs', component: () => import('./views/Logs.vue'), meta: { title: '日志', perm: 'log:read' } }
]