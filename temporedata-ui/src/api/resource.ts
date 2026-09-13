/** Resource center API. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'
import type { PageQuery, PageResult } from '@/types/base'

export interface ResourceEntity {
  id: string
  name?: string
  status?: string
  description?: string
}

export const resourceApi = {
  page: (params?: PageQuery) => apiGet<PageResult<ResourceEntity>>('/resource/page', params),
  list: () => apiGet<ResourceEntity[]>('/resource/list'),
  get: (id: string) => apiGet<ResourceEntity>(`/resource/${id}`),
  create: (d: Partial<ResourceEntity>) => apiPost<ResourceEntity>('/resource', d),
  update: (d: Partial<ResourceEntity>) => apiPut<ResourceEntity>('/resource', d),
  remove: (id: string) => apiDelete<void>(`/resource/${id}`),
}