import { NAV } from '../../layouts/navigation'

export const routes = [
  { path: NAV.data.path, name: 'V2.Datasources', component: () => import('./views/Datasources.vue'), meta: { title: '数据源', perm: 'data:read' } },
  { path: '/v2/data/metadata', name: 'V2.Metadata', component: () => import('./views/Metadata.vue'), meta: { title: '元数据', perm: 'data:read' } },
  { path: '/v2/data/lineage', name: 'V2.Lineage', component: () => import('./views/LineageView.vue'), meta: { title: '血缘', perm: 'data:read' } }
]