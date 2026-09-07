import api from '../index'

export const userApi = {
  list: () => api.get('/api/user').then(r => r.data.data),
  get: (id) => api.get(`/api/user/${id}`).then(r => r.data.data),
  me: () => api.get('/api/user/me').then(r => r.data.data),
  create: (data) => api.post('/api/user', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/user/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/user/${id}`).then(r => r.data.data),
  setRoles: (id, roleIds) => api.put(`/api/user/${id}/roles`, roleIds).then(r => r.data.data),
  updateProfile: (data) => api.put('/api/user/me/profile', data).then(r => r.data.data),
  changePassword: (data) => api.post('/api/user/me/password', data).then(r => r.data.data),
  changePhone: (data) => api.put('/api/user/me/phone', data).then(r => r.data.data),
  changeEmail: (data) => api.put('/api/user/me/email', data).then(r => r.data.data),
  ping: () => api.get('/api/user/ping').then(r => r.data.data)
}