import { NAV } from '../../layouts/navigation'

export const routes = [
  { path: NAV.admin.path, name: 'V2.Admin', component: () => import('./views/Admin.vue'), meta: { title: '管理', perm: 'admin:read' } }
]