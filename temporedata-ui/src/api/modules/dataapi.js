import api from '../index'

export const dataApiApi = {
  list: () => api.get('/api/services').then(r => r.data?.data || r.data),
  detail: (id) => api.get(`/api/services/${id}`).then(r => r.data?.data || r.data),
  create: (data) => api.post('/api/services', data).then(r => r.data?.data || r.data),
  update: (id, data) => api.put(`/api/services/${id}`, data).then(r => r.data?.data || r.data),
  delete: (id) => api.delete(`/api/services/${id}`).then(r => r.data?.data || r.data),
  toggle: (id) => api.put(`/api/services/${id}/toggle`).then(r => r.data?.data || r.data),
  regenerateKey: (id) => api.put(`/api/services/${id}/regenerate-key`).then(r => r.data?.data || r.data),
  test: (id) => api.post(`/api/services/${id}/test`).then(r => r.data?.data || r.data)
}