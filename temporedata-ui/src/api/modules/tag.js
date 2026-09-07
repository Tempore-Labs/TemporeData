import api from '../index'

export const tagApi = {
  list: () => api.get('/api/tag/list').then(r => r.data.data),
  create: (data) => api.post('/api/tag/create', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/tag/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/tag/${id}`).then(r => r.data.data),
  submit: (id) => api.post(`/api/tag/${id}/submit`).then(r => r.data.data),
  approve: (id) => api.post(`/api/tag/${id}/approve`).then(r => r.data.data),
  reject: (id) => api.post(`/api/tag/${id}/reject`).then(r => r.data.data),
  bind: (data) => api.post('/api/tag/bind', data).then(r => r.data.data),
  unbind: (tagId, assetType, assetId) =>
    api.delete('/api/tag/unbind', { params: { tagId, assetType, assetId } }).then(r => r.data.data),
  bindings: (assetType, assetId) =>
    api.get('/api/tag/bindings', { params: { assetType, assetId } }).then(r => r.data.data),
  bindingsAll: () => api.get('/api/tag/bindings/all').then(r => r.data.data),
  classifications: () => api.get('/api/tag/classification/list').then(r => r.data.data),
  createClassification: (name, code, description) =>
    api.post('/api/tag/classification/create', null, { params: { name, code, description } }).then(r => r.data.data),
  deleteClassification: (id) => api.delete(`/api/tag/classification/${id}`).then(r => r.data.data)
}