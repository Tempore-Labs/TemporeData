import api from '../index'

export const syncApi = {
  list: () => api.get('/api/sync').then(r => r.data.data),
  create: (data) => api.post('/api/sync', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/sync/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/sync/${id}`).then(r => r.data.data),
  execute: (id) => api.post(`/api/sync/${id}/execute`).then(r => r.data.data)
}