import api from '../index'

export const globalvarApi = {
  list: (tenantId = 'DEFAULT') => api.get('/api/global-var', { params: { tenantId } }).then(r => r.data),
  asMap: (tenantId = 'DEFAULT') => api.get('/api/global-var/map', { params: { tenantId } }).then(r => r.data),
  create: (data) => api.post('/api/global-var', data).then(r => r.data),
  update: (id, data) => api.put(`/api/global-var/${id}`, data).then(r => r.data),
  delete: (id) => api.delete(`/api/global-var/${id}`).then(r => r.data)
}