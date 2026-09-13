/** Datasource API (used by SQL editor and data catalog). */
import { apiDelete, apiGet, apiPost, apiPut } from './http'
import type { PageQuery, PageResult } from '@/types/base'

export interface DatasourceEntity {
  id: string
  name: string
  type: string
  host?: string
  port?: number
  database?: string
  username?: string
  password?: string
  params?: string
  status?: string
  createTime?: string
}

export const datasourceApi = {
  page: (params?: PageQuery) => apiGet<PageResult<DatasourceEntity>>('/datasource/page', params),
  list: () => apiGet<DatasourceEntity[]>('/datasource/list'),
  get: (id: string) => apiGet<DatasourceEntity>(`/datasource/${id}`),
  create: (d: Partial<DatasourceEntity>) => apiPost<DatasourceEntity>('/datasource', d),
  update: (id: string, d: Partial<DatasourceEntity>) => apiPut<DatasourceEntity>(`/datasource/${id}`, d),
  remove: (id: string) => apiDelete<void>(`/datasource/${id}`),
  test: (id: string) => apiPost<{ ok: boolean; message?: string }>(`/datasource/${id}/test`),
  pluginTypes: () => apiGet<{ type: string; label: string }[]>('/datasource/plugins/types'),
}
