/** Contract API. */
import { apiGet, apiPost } from './http'
import type { ContractCheckResult, ContractEntity } from '@/types/contract'

export const contractApi = {
  create: (datasetId: number, contractYaml: string, mode?: string, owner?: string) =>
    apiPost<ContractEntity>(
      '/contract',
      { datasetId, contractYaml, compatibilityMode: mode, owner },
      true,
    ),
  activate: (id: number) => apiPost<ContractEntity>(`/contract/${id}/activate`),
  check: (datasetId: number, promised: string, provided?: string) =>
    apiPost<ContractCheckResult>('/contract/check', { datasetId, promised, provided }, true),
  list: () => apiGet<ContractEntity[]>('/contract/list'),
  byDataset: (datasetId: number) => apiGet<ContractEntity>(`/contract/dataset/${datasetId}`),
}
