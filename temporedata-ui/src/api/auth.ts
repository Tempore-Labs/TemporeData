/** Auth API. */
import { apiPost } from './http'
import type { LoginReq, LoginRes } from '@/types/auth'

export const authApi = {
  login: (req: LoginReq) => apiPost<LoginRes>('/auth/login', req),
  logout: () => apiPost<void>('/auth/logout'),
}
