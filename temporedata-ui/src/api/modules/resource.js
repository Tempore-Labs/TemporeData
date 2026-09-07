import api from '../index'

export const resourceApi = {
  list: (tenantId = 'DEFAULT') => api.get('/api/resource', { params: { tenantId } }).then(r => r.data),
  listByType: (type, tenantId = 'DEFAULT') => api.get(`/api/resource/type/${type}`, { params: { tenantId } }).then(r => r.data),
  create: (data) => api.post('/api/resource', data).then(r => r.data),
  update: (id, data) => api.put(`/api/resource/${id}`, data).then(r => r.data),
  delete: (id) => api.delete(`/api/resource/${id}`).then(r => r.data)
}