import api from '../index'

export const roleApi = {
  list: () => api.get('/api/role').then(r => r.data.data),
  create: (data) => api.post('/api/role', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/role/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/role/${id}`).then(r => r.data.data),
  members: (id) => api.get(`/api/role/${id}/members`).then(r => r.data.data)
}
