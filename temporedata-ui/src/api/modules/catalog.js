import api from '../index'

export const catalogApi = {
  list: () => api.get('/api/catalog').then(r => r.data.data),
  detail: (id) => api.get(`/api/catalog/${id}`).then(r => r.data.data),
  tree: () => api.get('/api/catalog/tree').then(r => r.data.data),
  syncAll: () => api.post('/api/catalog/sync-all').then(r => r.data.data),
  lineage: (id) => api.get(`/api/catalog/lineage/${id}`).then(r => r.data.data),
  updateComment: (tableId, columnId, comment) =>
    api.put('/api/catalog/comment', { tableId, columnId, comment }).then(r => r.data.data),
  updateGovernance: (data) => api.put('/api/catalog/governance', data).then(r => r.data.data)
}