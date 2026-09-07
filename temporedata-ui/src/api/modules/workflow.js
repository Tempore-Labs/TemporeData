import api from '../index'

export const workflowApi = {
  list: () => api.get('/api/workflow').then(r => r.data.data),
  detail: (id) => api.get(`/api/workflow/${id}`).then(r => r.data.data),
  create: (data) => api.post('/api/workflow', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/workflow/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/workflow/${id}`).then(r => r.data.data),
  execute: (id) => api.post(`/api/workflow/${id}/execute`).then(r => r.data.data),
  schedule: (id, data) => api.put(`/api/workflow/${id}/schedule`, data).then(r => r.data),
  disableSchedule: (id) => api.put(`/api/workflow/${id}/schedule`, { enabled: 0 }).then(r => r.data),
  executions: (id) => api.get(`/api/workflow/${id}/executions`).then(r => r.data.data),

  // P0-2 real DAG runtime
  run: (id) => api.post(`/api/workflow/${id}/run`).then(r => r.data.data),
  instances: (id) => api.get(`/api/workflow/${id}/instances`).then(r => r.data.data),
  instanceDetail: (id) => api.get(`/api/workflow/instances/${id}`).then(r => r.data.data),
  instanceLogs: (instanceId) => api.get(`/api/workflow/instances/${instanceId}/logs`).then(r => r.data.data),

  // P0-3 runtime management
  pauseInstance: (id) => api.post(`/api/workflow/instances/${id}/pause`).then(r => r.data.data),
  resumeInstance: (id) => api.post(`/api/workflow/instances/${id}/resume`).then(r => r.data.data),
  stopInstance: (id) => api.post(`/api/workflow/instances/${id}/stop`).then(r => r.data.data),
  rerunInstance: (id, data = {}) => api.post(`/api/workflow/instances/${id}/rerun`, data).then(r => r.data.data),
  setInstancePriority: (id, priority) => api.put(`/api/workflow/instances/${id}/priority`, { priority }).then(r => r.data.data),
  setInstancePool: (id, pool) => api.put(`/api/workflow/instances/${id}/pool`, { pool }).then(r => r.data.data),
  instanceCommands: (id) => api.get(`/api/workflow/instances/${id}/commands`).then(r => r.data.data)
}