import { NAV } from '../../layouts/navigation'

export const routes = [
  { path: NAV.monitoring.path, name: 'V2.Monitoring', component: () => import('./views/Monitoring.vue'), meta: { title: '监控', perm: 'metric:read' } }
]