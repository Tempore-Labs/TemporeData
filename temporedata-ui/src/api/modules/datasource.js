import api from '../index'

export const datasourceApi = {
  list: () => api.get('/api/datasource').then(r => r.data.data),
  create: (data) => api.post('/api/datasource', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/datasource/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/datasource/${id}`).then(r => r.data.data),
  test: (id) => api.post(`/api/datasource/${id}/test`).then(r => r.data.data)
}