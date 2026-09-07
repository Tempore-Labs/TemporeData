import { NAV } from '../../layouts/navigation'

export const routes = [
  { path: NAV.dashboard.path, name: 'V2.Dashboard', component: () => import('./views/DashboardHome.vue'), meta: { title: '总览', perm: 'dashboard:read' } }
]