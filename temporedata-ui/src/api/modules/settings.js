import api from '../index'

export const settingsApi = {
  list: () => api.get('/api/settings').then(r => r.data),
  getGroup: (groupName) => api.get(`/api/settings/group/${groupName}`).then(r => r.data),
  getValue: (key) => api.get(`/api/settings/${key}`).then(r => r.data),
  saveGroup: (groupName, configs) => api.post(`/api/settings/group/${groupName}`, configs).then(r => r.data),
  setValue: (key, data) => api.put(`/api/settings/${key}`, data).then(r => r.data),
  delete: (key) => api.delete(`/api/settings/${key}`).then(r => r.data)
}