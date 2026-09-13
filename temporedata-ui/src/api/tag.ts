/** Data tag & indicator API. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'
import type { PageQuery, PageResult } from '@/types/base'

export interface TagEntity {
  id: string
  name?: string
  code?: string
  description?: string
  color?: string
  status?: string
  isTerm?: boolean
  classificationId?: string
  classificationName?: string
  isMutuallyExclusive?: boolean
  synonyms?: string
  createTime?: string
  updateTime?: string
}

export interface IndicatorEntity {
  id: string
  name?: string
  code?: string
  type?: string
  status?: string
  description?: string
  querySql?: string
  datasourceId?: string
  unit?: string
  owner?: string
  latestStatus?: string
  latestValue?: string
  latestExecuteTime?: string
  latestDurationMs?: number
  latestErrorMsg?: string
  totalRuns?: number
  successRuns?: number
  failedRuns?: number
  createTime?: string
}

export const tagApi = {
  page: (params?: PageQuery) => apiGet<PageResult<TagEntity>>('/tag/page', params),
  list: () => apiGet<TagEntity[]>('/tag/list'),
  get: (id: string) => apiGet<TagEntity>(`/tag/${id}`),
  create: (d: Partial<TagEntity>) => apiPost<TagEntity>('/tag', d),
  update: (d: Partial<TagEntity>) => apiPut<TagEntity>('/tag', d),
  remove: (id: string) => apiDelete<void>(`/tag/${id}`),
}

export const indicatorApi = {
  // Backend exposes paginated list as GET /indicator/list?page=&size= (Spring Pageable),
  // create as POST /indicator/create, update as PUT /indicator/{id}.
  page: (params?: PageQuery) => apiGet<PageResult<IndicatorEntity>>('/indicator/list', params),
  list: () => apiGet<IndicatorEntity[]>('/indicator/list'),
  get: (id: string) => apiGet<IndicatorEntity>(`/indicator/${id}`),
  create: (d: Partial<IndicatorEntity>) => apiPost<IndicatorEntity>('/indicator/create', d),
  update: (d: Partial<IndicatorEntity>) => apiPut<IndicatorEntity>(`/indicator/${d.id}`, d),
  remove: (id: string) => apiDelete<void>(`/indicator/${id}`),
}