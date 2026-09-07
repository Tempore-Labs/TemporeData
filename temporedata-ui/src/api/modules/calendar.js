import api from '../index'

// P1-4 business calendar REST API (/api/calendar)
export const calendarApi = {
  list: () => api.get('/api/calendar').then(r => r.data.data),
  get: (id) => api.get(`/api/calendar/${id}`).then(r => r.data.data),
  create: (data) => api.post('/api/calendar', data).then(r => r.data.data),
  update: (id, data) => api.put(`/api/calendar/${id}`, data).then(r => r.data.data),
  delete: (id) => api.delete(`/api/calendar/${id}`).then(r => r.data.data),
  setDays: (id, days) => api.post(`/api/calendar/${id}/days`, days).then(r => r.data.data),
  setCut: (id, cutHour, cutMinute) => api.put(`/api/calendar/${id}/cut`, { cutHour, cutMinute }).then(r => r.data.data),
  preview: (id, from, to) => api.get(`/api/calendar/${id}/preview`, { params: { from, to } }).then(r => r.data.data)
}