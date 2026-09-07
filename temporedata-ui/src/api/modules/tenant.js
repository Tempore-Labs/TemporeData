import api from '../index'
export const tenantApi = {
  list: () => api.get('/api/tenant').then(r => r.data.data),
  create: (data) => api.post('/api/tenant', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/tenant/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/tenant/${id}`).then(r => r.data.data),
  listMembers: (tenantId) => api.get(`/api/tenant/${tenantId}/members`).then(r => r.data.data),
  addMember: (tenantId, data) => api.post(`/api/tenant/${tenantId}/members`, data).then(r => r.data.data),
  removeMember: (tenantId, userId) => api.delete(`/api/tenant/${tenantId}/members/${userId}`).then(r => r.data.data),
  setMemberStatus: (tenantId, userId, status) => api.put(`/api/tenant/${tenantId}/members/${userId}/status`, null, { params: { status } }).then(r => r.data.data),
}