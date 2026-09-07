/**
 * http client for the v2.0 feature modules.
 * Single axios instance, baseURL /api/v1, attaches JWT, and normalizes
 * ApiResponse<T> { code, msg, data, requestId, traceId } + maps error codes.
 * Feature views must call feature `api.js` (which uses this), never axios directly.
 */
import axios from 'axios'

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 30000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('td_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  // Idempotency: one key per intent, reused on retries by the caller via config.
  if (!config.headers['Idempotency-Key'] && config.method !== 'get') {
    config.headers['Idempotency-Key'] = crypto.randomUUID?.() || Date.now().toString()
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    // Accept wrapped ApiResponse OR raw data.
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) {
        return Promise.reject({ message: body.msg || 'request failed', code: body.code, requestId: body.requestId, traceId: body.traceId })
      }
      return body.data
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    if (status === 401) { /* auth-store logout hook—wired later */ }
    return Promise.reject({ message: err.message, status, traceId: err.response?.headers?.['x-request-id'] })
  }
)

export default http