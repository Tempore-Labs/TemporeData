/** Black/white list API. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'
import type { PageQuery, PageResult } from '@/types/base'

export interface BlacklistEntity {
  id: string
  ipAddress?: string
  listType?: string
  reason?: string
  status?: string
  createTime?: string
  updateTime?: string
}

export const blacklistApi = {
  page: (params?: PageQuery) => apiGet<PageResult<BlacklistEntity>>('/blacklist/page', params),
  list: () => apiGet<BlacklistEntity[]>('/blacklist/list'),
  get: (id: string) => apiGet<BlacklistEntity>(`/blacklist/${id}`),
  create: (d: Partial<BlacklistEntity>) => apiPost<BlacklistEntity>('/blacklist', d),
  update: (d: Partial<BlacklistEntity>) => apiPut<BlacklistEntity>('/blacklist', d),
  remove: (id: string) => apiDelete<void>(`/blacklist/${id}`),
}