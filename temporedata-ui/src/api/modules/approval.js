import api from '../index'

export const approvalApi = {
  list: () => api.get('/api/approval/list').then(r => r.data.data),
  create: (data) => api.post('/api/approval/create', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/approval/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/approval/${id}`).then(r => r.data.data),
  approve: (id) => api.post(`/api/approval/${id}/approve`).then(r => r.data.data),
  reject: (id) => api.post(`/api/approval/${id}/reject`).then(r => r.data.data)
}