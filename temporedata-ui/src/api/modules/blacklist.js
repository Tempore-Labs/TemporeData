import api from '../index'

export const blacklistApi = {
  list: () => api.get('/api/blacklist/list').then(r => r.data.data),
  create: (data) => api.post('/api/blacklist/create', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/blacklist/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/blacklist/${id}`).then(r => r.data.data),
  enable: (id) => api.post(`/api/blacklist/${id}/enable`).then(r => r.data.data),
  disable: (id) => api.post(`/api/blacklist/${id}/disable`).then(r => r.data.data),
  check: (ip) => api.get('/api/blacklist/check', { params: { ip } }).then(r => r.data.data)
}