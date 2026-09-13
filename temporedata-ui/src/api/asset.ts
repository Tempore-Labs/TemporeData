/** Asset API. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'
import type { PageQuery, PageResult } from '@/types/base'
import type { AssetCatalogEntity, MetricEntity } from '@/types/asset'

export const assetApi = {
  catalogPage: (params?: PageQuery) => apiGet<PageResult<AssetCatalogEntity>>('/asset/catalog/page', params),
  catalogGet: (datasetId: number) => apiGet<AssetCatalogEntity>(`/asset/catalog/${datasetId}`),
  catalogCreate: (d: Partial<AssetCatalogEntity>) => apiPost<AssetCatalogEntity>('/asset/catalog', d),
  catalogSearch: (tag?: string) => apiGet<AssetCatalogEntity[]>('/asset/catalog/search', { tag }),
  catalogTouch: (datasetId: number) => apiPost<void>(`/asset/catalog/${datasetId}/touch`),
  metricPage: (params?: PageQuery) => apiGet<PageResult<MetricEntity>>('/asset/metric/page', params),
  metricGet: (code: string) => apiGet<MetricEntity>(`/asset/metric/${code}`),
  metricCreate: (d: Partial<MetricEntity>) => apiPost<MetricEntity>('/asset/metric', d),
  metricUpdate: (id: number, d: Partial<MetricEntity>) => apiPut<MetricEntity>(`/asset/metric/${id}`, d),
  metricDelete: (id: number) => apiDelete<void>(`/asset/metric/${id}`),
}
