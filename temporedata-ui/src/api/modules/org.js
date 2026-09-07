import api from '../index'

export const orgApi = {
  list: (tenantId = 'DEFAULT') => api.get('/api/org', { params: { tenantId } }).then(r => r.data),
  tree: (tenantId = 'DEFAULT') => api.get('/api/org/tree', { params: { tenantId } }).then(r => r.data),
  create: (data) => api.post('/api/org', data).then(r => r.data),
  update: (id, data) => api.put(`/api/org/${id}`, data).then(r => r.data),
  delete: (id) => api.delete(`/api/org/${id}`).then(r => r.data),
  members: (orgId) => api.get(`/api/org/${orgId}/members`).then(r => r.data.data),
  assignMember: (orgId, userId) => api.post(`/api/org/${orgId}/members/${userId}`).then(r => r.data.data),
  removeMember: (orgId, userId) => api.delete(`/api/org/${orgId}/members/${userId}`).then(r => r.data.data),
  setResponsible: (orgId, userId) => api.put(`/api/org/${orgId}/responsible/${userId}`).then(r => r.data.data),
  transferMember: (userId, newOrgId) => api.put(`/api/org/transfer/${userId}`, null, { params: { newOrgId } }).then(r => r.data.data)
}