import api from '../index'
export const clusterApi = {
  list: () => api.get('/api/cluster').then(r => r.data.data),
  detail: (id) => api.get(`/api/cluster/${id}`).then(r => r.data.data),
  create: (data) => api.post('/api/cluster', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/cluster/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/cluster/${id}`).then(r => r.data.data),
  listNodes: (clusterId) => api.get(`/api/cluster/${clusterId}/nodes`).then(r => r.data.data),
  addNode: (clusterId, data) => api.post(`/api/cluster/${clusterId}/nodes`, data).then(r => r.data.data),
  installAgent: (nodeId) => api.post(`/api/cluster/nodes/${nodeId}/install`).then(r => r.data.data),
  checkStatus: (nodeId) => api.get(`/api/cluster/nodes/${nodeId}/status`).then(r => r.data.data),
}