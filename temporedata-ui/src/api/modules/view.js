import api from '../index'

export const viewApi = {
  list: () => api.get('/api/view').then(r => r.data.data),
  get: (id) => api.get(`/api/view/${id}`).then(r => r.data.data),
  create: (data) => api.post('/api/view', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/view/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/view/${id}`).then(r => r.data.data),
  publish: (id) => api.post(`/api/view/${id}/publish`).then(r => r.data.data),
  execute: (id) => api.post(`/api/view/${id}/execute`).then(r => r.data.data),
}