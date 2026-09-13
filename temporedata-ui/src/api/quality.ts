/** Quality API. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'
import type { PageQuery, PageResult } from '@/types/base'
import type { QualityAssessResult, QualityGateEntity, QualityRuleEntity } from '@/types/quality'

export const qualityApi = {
  rulePage: (params?: PageQuery) => apiGet<PageResult<QualityRuleEntity>>('/quality/rule/page', params),
  ruleList: () => apiGet<QualityRuleEntity[]>('/quality/rule/list'),
  createRule: (d: Partial<QualityRuleEntity>) => apiPost<QualityRuleEntity>('/quality/rule', d),
  updateRule: (id: number, d: Partial<QualityRuleEntity>) => apiPut<QualityRuleEntity>(`/quality/rule/${id}`, d),
  deleteRule: (id: number) => apiDelete<void>(`/quality/rule/${id}`),
  gateList: () => apiGet<QualityGateEntity[]>('/quality/gate/list'),
  createGate: (d: Partial<QualityGateEntity>) => apiPost<QualityGateEntity>('/quality/gate', d),
  assess: (datasetId: number, score: number) =>
    apiPost<QualityAssessResult>(`/quality/dataset/${datasetId}/assess`, { score }, true),
}
