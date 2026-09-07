import api from '../index'

export const sensitiveApi = {
  list: () => api.get('/api/sensitive/list').then(r => r.data.data),
  create: (data) => api.post('/api/sensitive/create', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/sensitive/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/sensitive/${id}`).then(r => r.data.data),
  discover: () => api.post('/api/sensitive/discover').then(r => r.data.data),
  applyMask: (data) => api.post('/api/sensitive/apply-mask', data).then(r => r.data.data)
}