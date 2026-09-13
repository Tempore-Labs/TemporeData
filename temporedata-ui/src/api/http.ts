/**
 * Axios instance for the TemporeData backend.
 * - baseURL `/api` (dev: proxied to localhost:8090 by Vite; prod: same-origin or CORS)
 * - request interceptor injects `Authorization: Bearer <token>`
 * - response interceptor unwraps `BaseResponse{code,msg,data}`; code!==0 -> error toast
 * - 401 -> clear session and redirect to /login
 */
import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { BaseResponse } from '@/types/base'

const TOKEN_KEY = 'td_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('td_user')
}

const http: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

http.interceptors.response.use(
  (response: AxiosResponse<BaseResponse>) => {
    const body = response.data
    // Non-standard responses (blob downloads etc.) pass through untouched.
    if (body == null || typeof body !== 'object' || !('code' in body)) {
      return response
    }
    if (body.code !== 0) {
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg || `code ${body.code}`))
    }
    return response
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      clearToken()
      if (!window.location.hash.startsWith('#/login')) {
        ElMessage.warning('登录已过期，请重新登录')
        window.location.hash = '#/login'
      }
    } else if (status === 403) {
      ElMessage.error('无权限执行该操作')
    } else {
      ElMessage.error(error?.response?.data?.msg || error?.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

/** Typed helper: GET and return `data` from the BaseResponse envelope. */
export async function apiGet<T>(url: string, params?: object): Promise<T> {
  const res = await http.get<BaseResponse<T>>(url, { params })
  return res.data.data
}

/** Typed helper: POST with JSON body (or form params when `isForm`), return `data`. */
export async function apiPost<T>(url: string, body?: object, isForm = false): Promise<T> {
  const res = await http.post<BaseResponse<T>>(url, isForm ? undefined : body, isForm ? { params: body } : undefined)
  return res.data.data
}

/** Typed helper: PUT, return `data`. */
export async function apiPut<T>(url: string, body?: object, isForm = false): Promise<T> {
  const res = await http.put<BaseResponse<T>>(url, isForm ? undefined : body, isForm ? { params: body } : undefined)
  return res.data.data
}

/** Typed helper: DELETE, return `data`. */
export async function apiDelete<T>(url: string, params?: object): Promise<T> {
  const res = await http.delete<BaseResponse<T>>(url, { params })
  return res.data.data
}

export default http
