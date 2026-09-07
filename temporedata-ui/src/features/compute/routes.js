import { NAV } from '../../layouts/navigation'

export const routes = [
  { path: NAV.compute.path, name: 'V2.Compute', component: () => import('./views/Compute.vue'), meta: { title: '计算', perm: 'compute:read' } }
]