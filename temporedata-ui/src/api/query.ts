/** Governed query execution API (POST /api/query/execute). */
import http from './http'
import { apiGet, apiPost } from './http'
import type { BaseResponse, PageQuery, PageResult } from '@/types/base'

export interface QueryResult {
  id: string
  sql: string
  datasourceId?: string
  datasourceName?: string
  status: 'SUCCESS' | 'FAILED'
  durationMs: number
  rowCount: number
  errorMsg?: string
  columns?: string[]
  rows?: Record<string, unknown>[]
  createTime?: string
}

export const queryApi = {
  execute: (sql: string, datasourceId: string, datasourceName: string) =>
    apiPost<QueryResult>('/query/execute', { sql, datasourceId, datasourceName }),
  history: (params?: PageQuery) => apiGet<PageResult<QueryResult>>('/query/history', params),
}

/** Raw post (no BaseResponse envelope helpers) if needed elsewhere. */
export const queryHttp = http
export type { BaseResponse }
