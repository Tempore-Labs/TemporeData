import api from '../index'
export const secretApi = {
  list: () => api.get('/api/secret').then(r => r.data.data),
  create: (data) => api.post('/api/secret', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/secret/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/secret/${id}`).then(r => r.data.data),
}