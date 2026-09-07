import api from '../index'

export const haApi = {
  getServers: () => api.get('/api/ha/servers').then(r => r.data.data),
  getActiveServers: () => api.get('/api/ha/servers/active').then(r => r.data.data),
  recover: () => api.post('/api/ha/recover').then(r => r.data.data)
}