import api from '../index'

export const alarmApi = {
  list: () => api.get('/api/alarm/config').then(r => r.data?.data || r.data),
  create: (data) => api.post('/api/alarm/config', data).then(r => r.data?.data || r.data),
  update: (id, data) => api.put(`/api/alarm/config/${id}`, data).then(r => r.data?.data || r.data),
  delete: (id) => api.delete(`/api/alarm/config/${id}`).then(r => r.data?.data || r.data),
  detail: (id) => api.get(`/api/alarm/config/${id}`).then(r => r.data?.data || r.data),
  records: (id) => api.get(`/api/alarm/config/${id}/records`).then(r => r.data?.data || r.data),
  test: (id) => api.post(`/api/alarm/config/${id}/test`).then(r => r.data?.data || r.data)
}

// P2-8 baseline alarm endpoints (基线与告警记录)
export const baselineApi = {
  baselines: () => api.get('/api/alarm/baselines').then(r => r.data.data),
  create: (data) => api.post('/api/alarm/baselines', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/alarm/baselines/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/alarm/baselines/${id}`).then(r => r.data.data),
  toggle: (id, enabled) => api.post(`/api/alarm/baselines/${id}/enable`, null, { params: { enabled } }).then(r => r.data.data),
  test: (id) => api.post(`/api/alarm/baselines/${id}/test`).then(r => r.data.data),
  records: (baselineId, status) => api.get('/api/alarm/records', { params: { baselineId, status } }).then(r => r.data.data),
  ack: (id) => api.post(`/api/alarm/records/${id}/ack`).then(r => r.data.data),
  close: (id) => api.post(`/api/alarm/records/${id}/close`).then(r => r.data.data),
  runCheck: () => api.post('/api/alarm/run-check').then(r => r.data.data)
}