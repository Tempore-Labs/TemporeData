import api from '../index'

export const ingestionApi = {
  list: () => api.get('/api/ingestion').then(r => r.data.data),
  create: (data) => api.post('/api/ingestion', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/ingestion/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/ingestion/${id}`).then(r => r.data.data),
  execute: (id) => api.post(`/api/ingestion/${id}/execute`).then(r => r.data.data)
}