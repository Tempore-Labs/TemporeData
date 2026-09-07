import api from '../index'

export const engineApi = {
  listSpark: () => api.get('/api/engine/spark').then(r => r.data.data),
  createSpark: (data) => api.post('/api/engine/spark', data).then(r => r.data.data),
  updateSpark: (id, data) => api.put(`/api/engine/spark/${id}`, data).then(r => r.data.data),
  deleteSpark: (id) => api.delete(`/api/engine/spark/${id}`).then(r => r.data.data),
  listFlink: () => api.get('/api/engine/flink').then(r => r.data.data),
  createFlink: (data) => api.post('/api/engine/flink', data).then(r => r.data.data),
  updateFlink: (id, data) => api.put(`/api/engine/flink/${id}`, data).then(r => r.data.data),
  deleteFlink: (id) => api.delete(`/api/engine/flink/${id}`).then(r => r.data.data)
}