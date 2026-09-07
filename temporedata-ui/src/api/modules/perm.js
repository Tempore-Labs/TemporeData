import api from '../index'

// P3-11 permission center
export const permApi = {
  resources: (resourceType, name) => api.get('/api/perm/resources', { params: { resourceType, name } }).then(r => r.data.data),
  registerResource: (data) => api.post('/api/perm/resources', data).then(r => r.data.data),
  deleteResource: (id) => api.delete(`/api/perm/resources/${id}`).then(r => r.data.data),
  permissions: (roleIds) => api.get('/api/perm/permissions', { params: { roleIds } }).then(r => r.data.data),
  grant: (data) => api.post('/api/perm/permissions', data).then(r => r.data.data),
  revoke: (id) => api.delete(`/api/perm/permissions/${id}`).then(r => r.data.data),
  myPerms: (roleIds) => api.get('/api/perm/my/perms', { params: { roleIds } }).then(r => r.data.data),
  verify: (data) => api.post('/api/perm/verify', data).then(r => r.data.data)
}