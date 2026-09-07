import api from '../index'

export const workApi = {
  list: () => api.get('/api/work/list').then(r => r.data?.data || r.data),
  page: (workflowId, page, pageSize) => api.get('/api/work/page', { params: { workflowId, page, pageSize } }).then(r => r.data?.data || r.data),
  add: (data) => api.post('/api/work/add', data).then(r => r.data?.data || r.data),
  update: (data) => api.post('/api/work/update', data).then(r => r.data?.data || r.data),
  delete: (workId) => api.post('/api/work/delete', { workId }).then(r => r.data?.data || r.data),
  run: (workId) => api.post('/api/work/run', { workId }).then(r => r.data?.data || r.data),
  stop: (workId) => api.post('/api/work/stop', { workId }).then(r => r.data?.data || r.data),
  get: (workId) => api.get(`/api/work/${workId}`).then(r => r.data?.data || r.data),
  copy: (workId) => api.post('/api/work/copy', { workId }).then(r => r.data?.data || r.data),
  top: (workId) => api.post('/api/work/top', { workId }).then(r => r.data?.data || r.data),
  getStatus: (workId) => api.get('/api/work/status', { params: { workId } }).then(r => r.data?.data || r.data),
  instances: (workId) => api.get('/api/work/instances', { params: { workId } }).then(r => r.data?.data || r.data)
}