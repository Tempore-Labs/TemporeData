/**
 * Auth store: JWT token + user profile, persisted to localStorage
 * (`td_token` / `td_user`).
 */
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import { clearToken, setToken } from '@/api/http'
import type { LoginReq, UserInfo } from '@/types/auth'

const USER_KEY = 'td_user'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('td_token') as string | null,
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null') as UserInfo | null,
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    displayName: (s) => s.user?.nickname || s.user?.username || '未登录',
  },
  actions: {
    init() {
      this.token = localStorage.getItem('td_token')
      try {
        this.user = JSON.parse(localStorage.getItem(USER_KEY) || 'null')
      } catch {
        this.user = null
      }
    },
    async login(req: LoginReq) {
      const res = await authApi.login(req)
      this.token = res.token
      this.user = res.user
      setToken(res.token)
      localStorage.setItem(USER_KEY, JSON.stringify(res.user))
      return res
    },
    async logout() {
      try {
        await authApi.logout()
      } catch {
        // ignore network errors on logout
      }
      this.token = null
      this.user = null
      clearToken()
      localStorage.removeItem(USER_KEY)
    },
  },
})
