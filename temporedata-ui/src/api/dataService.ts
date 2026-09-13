/** Data service API. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'
import type { PageQuery, PageResult } from '@/types/base'

export interface DataApiEntity {
  id: string
  name?: string
  description?: string
  datasourceId?: string
  sql?: string
  method?: string
  path?: string
  status?: string
  apiKey?: string
  createTime?: string
}

export interface ApiLogEntity {
  id: string
  apiName?: string
  apiPath?: string
  method?: string
  requestIp?: string
  requestParams?: string
  responseCode?: number
  costTime?: number
  callerId?: string
  createTime?: string
}

export const dataServiceApi = {
  list: () => apiGet<DataApiEntity[]>('/services'),
  get: (id: string) => apiGet<DataApiEntity>(`/services/${id}`),
  create: (d: Partial<DataApiEntity>) => apiPost<DataApiEntity>('/services', d),
  update: (id: string, d: Partial<DataApiEntity>) => apiPut<DataApiEntity>(`/services/${id}`, d),
  remove: (id: string) => apiDelete<void>(`/services/${id}`),
  toggle: (id: string) => apiPut<DataApiEntity>(`/services/${id}/toggle`),
  regenerateKey: (id: string) => apiPut<DataApiEntity>(`/services/${id}/regenerate-key`),
  test: (id: string) => apiPost<unknown>(`/services/${id}/test`),
}

export const apiLogApi = {
  page: (params?: PageQuery) => apiGet<PageResult<ApiLogEntity>>('/apilog/page', params),
  list: () => apiGet<ApiLogEntity[]>('/apilog/list'),
  create: (d: Partial<ApiLogEntity>) => apiPost<ApiLogEntity>('/apilog', d),
}