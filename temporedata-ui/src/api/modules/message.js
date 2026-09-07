import api from '../index'

export const messageApi = {
  list: (userId, tenantId = 'DEFAULT') => api.get('/api/message', { params: { userId, tenantId } }).then(r => r.data.data),
  unreadCount: (userId) => api.get('/api/message/unread-count', { params: { userId } }).then(r => r.data.data),
  send: (data) => api.post('/api/message', data).then(r => r.data.data),
  markRead: (id) => api.put(`/api/message/${id}/read`).then(r => r.data.data),
  markAllRead: (userId) => api.put('/api/message/read-all', null, { params: { userId } }).then(r => r.data.data),
  delete: (id) => api.delete(`/api/message/${id}`).then(r => r.data.data)
}