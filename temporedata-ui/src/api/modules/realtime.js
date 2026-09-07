import api from '../index'
export const realtimeApi = {
  list: () => api.get('/api/realtime').then(r => r.data.data),
  detail: (id) => api.get(`/api/realtime/${id}`).then(r => r.data.data),
  create: (data) => api.post('/api/realtime', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/realtime/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/realtime/${id}`).then(r => r.data.data),
  start: (id) => api.post(`/api/realtime/${id}/start`).then(r => r.data.data),
  stop: (id) => api.post(`/api/realtime/${id}/stop`).then(r => r.data.data),
  savepoint: (id) => api.post(`/api/realtime/${id}/savepoint`).then(r => r.data.data),
  getLogs: (id) => api.get(`/api/realtime/${id}/logs`).then(r => r.data.data),
}