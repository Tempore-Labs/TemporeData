/** Data report API. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'

export interface ReportEntity {
  id: string
  name?: string
  description?: string
  type?: string
  config?: string
  datasourceId?: string
  status?: string
  createTime?: string
  updateTime?: string
}

export const reportApi = {
  list: () => apiGet<ReportEntity[]>('/report/list'),
  create: (d: Partial<ReportEntity>) => apiPost<ReportEntity>('/report/create', d),
  update: (id: string, d: Partial<ReportEntity>) => apiPut<ReportEntity>(`/report/${id}`, d),
  remove: (id: string) => apiDelete<void>(`/report/${id}`),
  publish: (id: string) => apiPost<ReportEntity>(`/report/${id}/publish`),
  unpublish: (id: string) => apiPost<ReportEntity>(`/report/${id}/unpublish`),
}