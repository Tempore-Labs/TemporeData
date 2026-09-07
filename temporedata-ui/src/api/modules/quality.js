import api from '../index'

export const qualityApi = {
  list: () => api.get('/api/quality').then(r => r.data.data),
  create: (data) => api.post('/api/quality', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/quality/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/quality/${id}`).then(r => r.data.data),
  execute: (id) => api.post(`/api/quality/${id}/execute`).then(r => r.data.data),
  run: (id) => api.post(`/api/quality/${id}/run`).then(r => r.data.data),
  report: (datasourceId, tableName) => api.get('/api/audit/report', { params: { datasourceId, tableName } }).then(r => r.data.data),
  batchExecute: (datasourceId, tableName) => api.post('/api/audit/execute', null, { params: { datasourceId, tableName } }).then(r => r.data.data)
}