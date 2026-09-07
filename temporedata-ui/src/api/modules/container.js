import api from '../index'

export const containerApi = {
  list: () => api.get('/api/container').then(r => r.data.data),
  listByCluster: (clusterId) => api.get(`/api/container/cluster/${clusterId}`).then(r => r.data.data),
  get: (id) => api.get(`/api/container/${id}`).then(r => r.data.data),
  create: (data) => api.post('/api/container', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/container/${id}`, data).then(r => r.data.data),
  start: (id) => api.post(`/api/container/${id}/start`).then(r => r.data.data),
  stop: (id) => api.post(`/api/container/${id}/stop`).then(r => r.data.data),
  delete: (id) => api.delete(`/api/container/${id}`).then(r => r.data.data),
}