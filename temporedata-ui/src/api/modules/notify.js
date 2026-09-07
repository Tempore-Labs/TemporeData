import api from '../index'

export const notifyApi = {
  list: (tenantId = 'DEFAULT') => api.get('/api/notify', { params: { tenantId } }).then(r => r.data),
  create: (data) => api.post('/api/notify', data).then(r => r.data),
  update: (id, data) => api.put(`/api/notify/${id}`, data).then(r => r.data),
  toggle: (id, enabled) => api.put(`/api/notify/${id}/toggle`, { enabled }).then(r => r.data),
  delete: (id) => api.delete(`/api/notify/${id}`).then(r => r.data)
}