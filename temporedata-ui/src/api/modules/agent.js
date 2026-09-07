import api from '../index'

export const agentApi = {
  chat: (data) => api.post('/api/agent/chat', data).then(r => r.data.data),
  getConfig: () => api.get('/api/agent/config').then(r => r.data.data),
  saveConfig: (data) => api.post('/api/agent/config', data).then(r => r.data.data),
  testConfig: (data) => api.post('/api/agent/config/test', data).then(r => r.data.data),
  sessions: () => api.get('/api/agent/sessions').then(r => r.data.data),
  history: (sessionId) => api.get(`/api/agent/history/${sessionId}`).then(r => r.data.data),
  deleteSession: (sessionId) => api.delete(`/api/agent/session/${sessionId}`).then(r => r.data.data)
}