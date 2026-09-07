import api from '../index'

export const queryApi = {
  execute: (sql, datasourceId) =>
    api.post('/api/query/execute', { sql, datasourceId }).then(r => r.data.data),
  history: (page = 0, size = 20) =>
    api.get('/api/query/history', { params: { page, size } }).then(r => r.data.data)
}