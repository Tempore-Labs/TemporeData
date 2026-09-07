import api from '../index'

// Datasource driver plugins: read-only list + GAV-based upload via /api/driver.
export const datasourcePluginApi = {
  list: () => api.get('/api/datasource/plugins').then(r => r.data),
  create: (data) => api.post('/api/driver', data).then(r => r.data)
}