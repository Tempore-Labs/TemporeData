import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('td_token') || '',
    user: JSON.parse(localStorage.getItem('td_user') || 'null'),
    isLoggedIn: !!localStorage.getItem('td_token')
  }),

  actions: {
    setAuth(token, user) {
      this.token = token
      this.user = user
      this.isLoggedIn = true
      localStorage.setItem('td_token', token)
      localStorage.setItem('td_user', JSON.stringify(user))
    },

    logout() {
      this.token = ''
      this.user = null
      this.isLoggedIn = false
      localStorage.removeItem('td_token')
      localStorage.removeItem('td_user')
    }
  }
})