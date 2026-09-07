import api from '../index'
export const fileApi = {
  list: (params) => api.get('/api/file', { params }).then(r => r.data.data),
  upload: (formData) => api.post('/api/file/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then(r => r.data.data),
  bundle: (formData) => api.post('/api/file/bundle', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then(r => r.data.data),
  meta: (id) => api.get(`/api/file/${id}/meta`).then(r => r.data.data),
  members: (id) => api.get(`/api/file/${id}/members`).then(r => r.data.data),
  delete: (id) => api.delete(`/api/file/${id}`).then(r => r.data.data),
  getDownloadUrl: (id) => `/api/file/${id}`,
  getOriginalUrl: (id) => `/api/file/${id}/original`,
  getMemberUrl: (id, name) => `/api/file/${id}/member?name=${encodeURIComponent(name)}`,
}