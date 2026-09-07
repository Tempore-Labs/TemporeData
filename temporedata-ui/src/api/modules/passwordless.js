import api from '../index'

export const passwordlessApi = {
  list: () => api.get('/api/passwordless/list').then(r => r.data.data),
  create: (data) => api.post('/api/passwordless/create', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/passwordless/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/passwordless/${id}`).then(r => r.data.data),
  enable: (id) => api.post(`/api/passwordless/${id}/enable`).then(r => r.data.data),
  disable: (id) => api.post(`/api/passwordless/${id}/disable`).then(r => r.data.data),
  sendCode: (data) => api.post('/api/passwordless/send-code', data).then(r => r.data.data),
  verify: (data) => api.post('/api/passwordless/verify', data).then(r => r.data.data)
}