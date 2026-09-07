import api from '../index'

export const reportApi = {
  list: () => api.get('/api/report/list').then(r => r.data.data),
  create: (data) => api.post('/api/report/create', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/report/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/report/${id}`).then(r => r.data.data),
  publish: (id) => api.post(`/api/report/${id}/publish`).then(r => r.data.data),
  unpublish: (id) => api.post(`/api/report/${id}/unpublish`).then(r => r.data.data)
}