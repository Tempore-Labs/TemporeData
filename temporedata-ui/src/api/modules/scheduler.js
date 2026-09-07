import api from '../index'

// P0-1 scheduler engine REST API (/api/scheduler)
export const schedulerApi = {
  // ---- Task definitions ----
  tasks: () => api.get('/api/scheduler/tasks').then(r => r.data.data),
  task: (id) => api.get(`/api/scheduler/tasks/${id}`).then(r => r.data.data),
  createTask: (data) => api.post('/api/scheduler/tasks', data).then(r => r.data.data),
  updateTask: (id, data) => api.put(`/api/scheduler/tasks/${id}`, data).then(r => r.data.data),
  deleteTask: (id) => api.delete(`/api/scheduler/tasks/${id}`).then(r => r.data.data),
  setEnabled: (id, enabled) => api.put(`/api/scheduler/tasks/${id}/enabled`, { enabled }).then(r => r.data.data),

  // ---- Trigger & instances ----
  trigger: (id) => api.post(`/api/scheduler/tasks/${id}/trigger`).then(r => r.data.data),
  instances: (id) => api.get(`/api/scheduler/tasks/${id}/instances`).then(r => r.data.data),
  logs: (instanceId) => api.get(`/api/scheduler/instances/${instanceId}/logs`).then(r => r.data.data)
}