/** Compute container API. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'
import type { PageQuery, PageResult } from '@/types/base'

export interface ContainerEntity {
  id: string
  name?: string
  status?: string
  description?: string
}

export const containerApi = {
  page: (params?: PageQuery) => apiGet<PageResult<ContainerEntity>>('/container/page', params),
  list: () => apiGet<ContainerEntity[]>('/container/list'),
  get: (id: string) => apiGet<ContainerEntity>(`/container/${id}`),
  create: (d: Partial<ContainerEntity>) => apiPost<ContainerEntity>('/container', d),
  update: (d: Partial<ContainerEntity>) => apiPut<ContainerEntity>('/container', d),
  remove: (id: string) => apiDelete<void>(`/container/${id}`),
}