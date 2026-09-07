import { NAV } from '../../layouts/navigation'

export const routes = [
  { path: NAV.clusters.path, name: 'V2.Clusters', component: () => import('./views/ClusterList.vue'), meta: { title: '集群', perm: 'cluster:read' } },
  { path: '/v2/clusters/:id', name: 'V2.ClusterDetail', component: () => import('./views/ClusterDetail.vue'), meta: { title: '集群详情', perm: 'cluster:read' } }
]