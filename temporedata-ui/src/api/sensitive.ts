/** Sensitive data governance API. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'
import type { PageQuery, PageResult } from '@/types/base'

export interface SensitiveEntity {
  id: string
  datasourceId?: string
  tableName?: string
  columnName?: string
  sensitiveType?: string
  maskRule?: string
  level?: string
  description?: string
  status?: string
  createTime?: string
  updateTime?: string
}

export interface MaskRuleEntity {
  id: string
  name?: string
  ruleType: string
  maskPattern?: string
  description?: string
  datasourceId?: string
  tableName?: string
  columnName?: string
  status?: number
}

export const sensitiveApi = {
  page: (params?: PageQuery) => apiGet<PageResult<SensitiveEntity>>('/sensitive/page', params),
  list: () => apiGet<SensitiveEntity[]>('/sensitive/list'),
  get: (id: string) => apiGet<SensitiveEntity>(`/sensitive/${id}`),
  create: (d: Partial<SensitiveEntity>) => apiPost<SensitiveEntity>('/sensitive', d),
  update: (d: Partial<SensitiveEntity>) => apiPut<SensitiveEntity>('/sensitive', d),
  remove: (id: string) => apiDelete<void>(`/sensitive/${id}`),
}

export const maskRuleApi = {
  list: () => apiGet<MaskRuleEntity[]>('/security/mask-rule'),
  get: (id: string) => apiGet<MaskRuleEntity>(`/security/mask-rule/${id}`),
  create: (d: Partial<MaskRuleEntity>) => apiPost<MaskRuleEntity>('/security/mask-rule', d),
  update: (id: string, d: Partial<MaskRuleEntity>) => apiPut<MaskRuleEntity>(`/security/mask-rule/${id}`, d),
  remove: (id: string) => apiDelete<void>(`/security/mask-rule/${id}`),
}