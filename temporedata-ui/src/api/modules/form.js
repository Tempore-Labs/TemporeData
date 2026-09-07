import api from '../index'

export const formApi = {
  list: () => api.get('/api/form/list').then(r => r.data.data),
  create: (data) => api.post('/api/form/create', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/form/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/form/${id}`).then(r => r.data.data),
  submissions: (id) => api.get(`/api/form/${id}/submissions`).then(r => r.data.data),
  shareToken: (id) => api.post(`/api/form/${id}/share-token`).then(r => r.data.data)
}