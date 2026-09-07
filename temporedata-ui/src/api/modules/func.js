import api from '../index'
export const funcApi = {
  list: () => api.get('/api/func').then(r => r.data.data),
  detail: (id) => api.get(`/api/func/${id}`).then(r => r.data.data),
  create: (data) => api.post('/api/func', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/func/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/func/${id}`).then(r => r.data.data),
  test: (id) => api.post(`/api/func/${id}/test`).then(r => r.data.data),
}