import api from '../index'

export const dependencyApi = {
  list: (tenantId = 'DEFAULT') => api.get('/api/dependency', { params: { tenantId } }).then(r => r.data),
  listByType: (type, tenantId = 'DEFAULT') => api.get(`/api/dependency/type/${type}`, { params: { tenantId } }).then(r => r.data),
  create: (data) => api.post('/api/dependency', data).then(r => r.data),
  update: (id, data) => api.put(`/api/dependency/${id}`, data).then(r => r.data),
  delete: (id) => api.delete(`/api/dependency/${id}`).then(r => r.data)
}