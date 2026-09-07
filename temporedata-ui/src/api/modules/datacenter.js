import api from '../index'

export const datacenterApi = {
  list: () => api.get('/api/datacenter/list').then(r => r.data.data),
  create: (data) => api.post('/api/datacenter/create', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/datacenter/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/datacenter/${id}`).then(r => r.data.data),
  overview: () => api.get('/api/datacenter/overview').then(r => r.data.data)
}