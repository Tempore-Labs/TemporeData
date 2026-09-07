import { NAV } from '../../layouts/navigation'

export const routes = [
  { path: NAV.incidents.path, name: 'V2.Incidents', component: () => import('./views/IncidentList.vue'), meta: { title: '事件', perm: 'incident:read' } },
  { path: '/v2/incidents/:id', name: 'V2.IncidentDetail', component: () => import('./views/IncidentDetail.vue'), meta: { title: '事件详情', perm: 'incident:read' } }
]