import api from '../index'

export const apilogApi = {
  list: () => api.get('/api/apilog/list').then(r => r.data.data),
  clear: (daysBefore) => api.delete('/api/apilog/clear', { params: { daysBefore: daysBefore || 30 } }).then(r => r.data.data)
}