/** Monitor API. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'
import type { PageQuery, PageResult } from '@/types/base'

export interface MonitorEntity {
  id: string
  name: string
  status?: string
  description?: string
  createTime?: string
}

export const monitorApi = {
  page: (params?: PageQuery) => apiGet<PageResult<MonitorEntity>>('/monitor/page', params),
  list: () => apiGet<MonitorEntity[]>('/monitor/list'),
  create: (d: Partial<MonitorEntity>) => apiPost<MonitorEntity>('/monitor', d),
  update: (d: Partial<MonitorEntity>) => apiPut<MonitorEntity>('/monitor', d),
  remove: (id: string) => apiDelete<void>(`/monitor/${id}`),
}
