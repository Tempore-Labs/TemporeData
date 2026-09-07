import api from '../index'

// P3-12 unified audit (hash-chain verified)
export const auditApi = {
  events: (module, operator) => api.get('/api/audit/events', { params: { module, operator } }).then(r => r.data.data),
  eventsPage: (eventType, operator, action, page, size, resourceType) =>
    api.get('/api/audit/events/page', { params: { eventType, operator, action, resourceType, page, size } }).then(r => r.data.data),
  get: (id) => api.get(`/api/audit/events/${id}`).then(r => r.data.data),
  record: (data) => api.post('/api/audit/events', data).then(r => r.data.data),
  submit: (data) => api.post('/api/audit/events/submit', data).then(r => r.data.data),
  verify: () => api.post('/api/audit/verify/range', {}).then(r => r.data.data),
  export: () => api.get('/api/audit/export').then(r => r.data.data),
  archive: () => api.post('/api/audit/archive').then(r => r.data.data),
  archives: () => api.get('/api/audit/archives').then(r => r.data.data),
  policies: () => api.get('/api/audit/policies').then(r => r.data.data),
  savePolicy: (data) => api.post('/api/audit/policies', data).then(r => r.data.data)
}