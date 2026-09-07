import api from '../index'

export const authApi = {
  login: (username, password) =>
    api.post('/api/auth/login', { username, password }).then(r => {
      const { token, user } = r.data.data
      localStorage.setItem('td_token', token)
      localStorage.setItem('td_user', JSON.stringify(user))
      return r.data.data
    }),
  logout: () => api.post('/api/auth/logout'),
  getUserInfo: () => api.get('/api/user/me').then(r => r.data.data),
  ping: () => api.get('/api/user/ping').then(r => r.data.data),
  adminOnly: () => api.get('/api/user/admin-only').then(r => r.data.data)
}