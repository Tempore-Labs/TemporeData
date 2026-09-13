/** High availability API. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'
import type { PageQuery, PageResult } from '@/types/base'

export interface HaEntity {
  id: string
  name?: string
  status?: string
  description?: string
}

export const haApi = {
  page: (params?: PageQuery) => apiGet<PageResult<HaEntity>>('/ha/page', params),
  list: () => apiGet<HaEntity[]>('/ha/list'),
  get: (id: string) => apiGet<HaEntity>(`/ha/${id}`),
  create: (d: Partial<HaEntity>) => apiPost<HaEntity>('/ha', d),
  update: (d: Partial<HaEntity>) => apiPut<HaEntity>('/ha', d),
  remove: (id: string) => apiDelete<void>(`/ha/${id}`),
}