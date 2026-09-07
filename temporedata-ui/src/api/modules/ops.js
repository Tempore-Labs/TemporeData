import api from '../index'

// P2-10 GitHub/GitLab Ops integration
export const opsApi = {
  providers: () => api.get('/api/ops/providers').then(r => r.data.data),
  repos: () => api.get('/api/ops/repos').then(r => r.data.data),
  bind: (data) => api.post('/api/ops/repos', data).then(r => r.data.data),
  unbind: (id) => api.delete(`/api/ops/repos/${id}`).then(r => r.data.data),
  sync: (id) => api.post(`/api/ops/repos/${id}/sync`).then(r => r.data.data),
  trigger: (id) => api.post(`/api/ops/repos/${id}/trigger`).then(r => r.data.data),
  builds: (repoId, status) => api.get('/api/ops/builds', { params: { repoId, status } }).then(r => r.data.data)
}