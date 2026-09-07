import api from '../index'

export const permapprovalApi = {
  my: () => api.get('/api/permapproval/my').then(r => r.data.data),
  pending: () => api.get('/api/permapproval/pending').then(r => r.data.data),
  create: (data) => api.post('/api/permapproval/create', data).then(r => r.data.data),
  approve: (id) => api.post(`/api/permapproval/${id}/approve`).then(r => r.data.data),
  reject: (id) => api.post(`/api/permapproval/${id}/reject`).then(r => r.data.data)
}