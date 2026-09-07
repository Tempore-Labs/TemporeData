import api from '../index'

export const mydataApi = {
  list: () => api.get('/api/mydata/list').then(r => r.data.data),
  favorite: (data) => api.post('/api/mydata/favorite', data).then(r => r.data.data),
  removeFavorite: (resourceId) => api.delete(`/api/mydata/favorite/${resourceId}`).then(r => r.data.data),
  grant: (data) => api.post('/api/mydata/grant', data).then(r => r.data.data)
}