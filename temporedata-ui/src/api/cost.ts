/** Cost API. */
import { apiGet, apiPost } from './http'
import type { CostEntity, CostSummary } from '@/types/cost'

export const costApi = {
  record: (datasetId: number, compute?: number, storage?: number, query?: number, date?: string) =>
    apiPost<CostEntity>('/cost/record', { datasetId, compute, storage, query, date }, true),
  list: (datasetId: number) => apiGet<CostEntity[]>(`/cost/dataset/${datasetId}`),
  latest: (datasetId: number) => apiGet<CostEntity>(`/cost/dataset/${datasetId}/latest`),
  summarize: (datasetId: number, from: string, to: string) =>
    apiGet<CostSummary>('/cost/summarize', { datasetId, from, to }),
}
