import api from '../index'

export const securityApi = {
  // ---- data masking ----
  listMasks: () => api.get('/api/security/masks').then(r => r.data?.data || r.data),
  updateMask: (id, data) => api.put(`/api/security/masks/${id}`, data).then(r => r.data?.data || r.data),
  createMask: (data) => api.post('/api/security/masks', data).then(r => r.data?.data || r.data),
  deleteMask: (id) => api.delete(`/api/security/masks/${id}`).then(r => r.data?.data || r.data),
  // ---- data sensitivity levels ----
  listLevels: () => api.get('/api/security/levels').then(r => r.data?.data || r.data),
  updateLevel: (id, data) => api.put(`/api/security/levels/${id}`, data).then(r => r.data?.data || r.data),
  createLevel: (data) => api.post('/api/security/levels', data).then(r => r.data?.data || r.data),
  deleteLevel: (id) => api.delete(`/api/security/levels/${id}`).then(r => r.data?.data || r.data),
  categoryTree: () => api.get('/api/security/category/tree').then(r => r.data?.data || r.data),
  // ---- P3-13 supply-chain & QA ----
  vulns: () => api.get('/api/security/vulns').then(r => r.data.data),
  createVuln: (data) => api.post('/api/security/vulns', data).then(r => r.data.data),
  updateStatus: (id, status) => api.post(`/api/security/vulns/${id}/status`, { status }).then(r => r.data.data),
  accept: (id, mitigation) => api.post(`/api/security/vulns/${id}/accept`, { mitigation }).then(r => r.data.data),
  sbom: (version) => api.get('/api/security/sbom', { params: { version } }).then(r => r.data.data),
  gate: () => api.get('/api/security/gate').then(r => r.data.data),
  qa: (version) => api.get('/api/security/qa', { params: { version } }).then(r => r.data.data),
  addQa: (data) => api.post('/api/security/qa', data).then(r => r.data.data)
}