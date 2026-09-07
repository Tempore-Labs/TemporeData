import api from '../index'

export const indicatorApi = {
  list: (page = 0, size = 20) => api.get('/api/indicator/list', { params: { page, size } }).then(r => r.data.data),
  stats: () => api.get('/api/indicator/stats').then(r => r.data.data),
  create: (data) => api.post('/api/indicator/create', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/indicator/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/indicator/${id}`).then(r => r.data.data),
  execute: (id) => api.post(`/api/indicator/${id}/execute`).then(r => r.data.data),
  runs: (id) => api.get(`/api/indicator/${id}/runs`).then(r => r.data.data),
  lineage: (id) => api.get(`/api/indicator/${id}/lineage`).then(r => r.data.data)
}