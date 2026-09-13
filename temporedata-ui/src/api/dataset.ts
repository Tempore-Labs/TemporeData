/** Dataset & metadata API. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'
import type { PageQuery, PageResult } from '@/types/base'
import type { DatasetEntity, MetadataVersionEntity } from '@/types/dataset'

export const datasetApi = {
  page: (params?: PageQuery) => apiGet<PageResult<DatasetEntity>>('/dataset/page', params),
  get: (id: number) => apiGet<DatasetEntity>(`/dataset/${id}`),
  byCode: (code: string) => apiGet<DatasetEntity>(`/dataset/byCode/${code}`),
  byDomain: (domainId: number) => apiGet<DatasetEntity[]>(`/dataset/domain/${domainId}`),
  create: (d: Partial<DatasetEntity>) => apiPost<DatasetEntity>('/dataset', d),
  update: (id: number, d: Partial<DatasetEntity>) => apiPut<DatasetEntity>(`/dataset/${id}`, d),
  remove: (id: number) => apiDelete<void>(`/dataset/${id}`),
  snapshot: (id: number, schemaJson: string, driftType?: string) =>
    apiPost<MetadataVersionEntity>(`/dataset/${id}/versions`, { schemaJson, driftType }, true),
  versions: (id: number) => apiGet<MetadataVersionEntity[]>(`/dataset/${id}/versions`),
  crawl: (datasetIds?: string) => apiPost<any>('/metadata/crawl', { datasetIds }, true),
}
