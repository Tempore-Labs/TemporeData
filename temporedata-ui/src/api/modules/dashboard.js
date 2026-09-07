import api from '../index'

export const dashboardApi = {
  list: () => api.get('/api/dashboard').then(r => r.data.data),
  get: () => api.get('/api/dashboard').then(r => r.data.data)
}