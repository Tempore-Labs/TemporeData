import api from '../index'

export const preferenceApi = {
  list: () => api.get('/api/preference/list').then(r => r.data.data),
  save: (data) => api.post('/api/preference/save', data).then(r => r.data.data),
  batchSave: (data) => api.post('/api/preference/batch-save', data).then(r => r.data.data)
}