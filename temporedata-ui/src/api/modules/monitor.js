import api from '../index'

export const monitorApi = {
  system: () => api.get('/api/monitor/system').then(r => r.data.data),
  cluster: (clusterId) => api.get('/api/monitor/cluster', { params: { clusterId } }).then(r => r.data.data),
  instance: () => api.get('/api/monitor/instance').then(r => r.data.data)
}