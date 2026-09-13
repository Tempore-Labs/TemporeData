/** Operations & governance module APIs: work/realtime/approval/file/meta. */
import { apiGet, apiPost, apiPut, apiDelete } from './http'
import type { PageQuery, PageResult } from '@/types/base'

/** Backend `PageRes<T>` — { items, total, page(1-based), size }. */
export interface PageRes<T> {
  items: T[]
  total: number
  page: number
  size: number
}

// ============ dev/work (作业管理) ============

export interface WorkEntity {
  id: string
  name?: string
  status?: string
  description?: string
}

export const workApi = {
  page: (params?: PageQuery) => apiGet<PageResult<WorkEntity>>('/work/page', params),
  list: () => apiGet<WorkEntity[]>('/work/list'),
  get: (id: string) => apiGet<WorkEntity>(`/work/${id}`),
  add: (d: Partial<WorkEntity>) => apiPost<WorkEntity>('/work/add', d),
  update: (d: Partial<WorkEntity>) => apiPost<WorkEntity>('/work/update', d),
  remove: (workId: string) => apiPost<void>('/work/delete', { workId }),
  run: (workId: string) => apiPost<WorkEntity>('/work/run', { workId }),
  stop: (workId: string) => apiPost<WorkEntity>('/work/stop', { workId }),
  copy: (workId: string) => apiPost<WorkEntity>('/work/copy', { workId }),
  top: (workId: string) => apiPost<WorkEntity>('/work/top', { workId }),
  status: (workId: string) => apiGet<Record<string, unknown>>('/work/status', { workId }),
  instances: (workId: string) =>
    apiGet<Array<Record<string, unknown>>>('/work/instances', { workId }),
}

// ============ ops/real (实时任务) ============

export interface RealEntity {
  id: string
  name?: string
  type?: string
  script?: string
  clusterId?: string
  status?: string
  createTime?: string
}

export const realtimeApi = {
  list: () => apiGet<RealEntity[]>('/realtime'),
  get: (id: string) => apiGet<RealEntity>(`/realtime/${id}`),
  create: (d: Partial<RealEntity>) => apiPost<RealEntity>('/realtime', d),
  update: (id: string, d: Partial<RealEntity>) => apiPut<RealEntity>(`/realtime/${id}`, d),
  remove: (id: string) => apiDelete<void>(`/realtime/${id}`),
  start: (id: string) => apiPost<RealEntity>(`/realtime/${id}/start`),
  stop: (id: string) => apiPost<RealEntity>(`/realtime/${id}/stop`),
  savepoint: (id: string) => apiPost<Record<string, unknown>>(`/realtime/${id}/savepoint`),
  logs: (id: string) => apiGet<string[]>(`/realtime/${id}/logs`),
}

// ============ dev/approval (审批中心) ============

export interface ApprovalEntity {
  id: string
  title?: string
  description?: string
  workflowId?: string
  applicantId?: string
  approverId?: string
  status?: string
  comment?: string
  createTime?: string
  updateTime?: string
}

export const approvalApi = {
  page: (params?: PageQuery) => apiGet<PageResult<ApprovalEntity>>('/approval/page', params),
  list: () => apiGet<ApprovalEntity[]>('/approval/list'),
  pending: () => apiGet<ApprovalEntity[]>('/approval/pending'),
  my: (applicantId?: string) => apiGet<ApprovalEntity[]>('/approval/my', { applicantId }),
  get: (id: string) => apiGet<ApprovalEntity>(`/approval/${id}`),
  create: (d: Partial<ApprovalEntity>) => apiPost<ApprovalEntity>('/approval', d),
  update: (id: string, d: Partial<ApprovalEntity>) => apiPut<ApprovalEntity>(`/approval/${id}`, d),
  approve: (id: string) => apiPost<ApprovalEntity>(`/approval/${id}/approve`),
  reject: (id: string) => apiPost<ApprovalEntity>(`/approval/${id}/reject`),
  remove: (id: string) => apiDelete<void>(`/approval/${id}`),
}

// ============ integration/file (文件中心) ============

export interface FileItem {
  id: string
  name?: string
  originalName?: string
  fileSize?: number
  fileType?: string
  ext?: string
  md5?: string
  bizType?: string
  bizId?: string
  createBy?: string
  createTime?: string
  imageOptimized?: boolean
  packaged?: boolean
  memberCount?: number
}

export const fileCenterApi = {
  page: (params?: PageQuery) =>
    apiGet<PageRes<FileItem>>('/file', { ...params, page: (params?.page ?? 0) + 1, size: params?.size ?? 10 }),
  meta: (id: string) => apiGet<FileItem>(`/file/${id}/meta`),
  remove: (id: string) => apiDelete<void>(`/file/${id}`),
  downloadUrl: (id: string) => `/api/file/${id}`,
  members: (id: string) => apiGet<string[]>(`/file/${id}/members`),
  stats: () => apiGet<Record<string, unknown>>('/file/stats'),
}

// ============ gov/meta (元数据采集) ============

export interface MetaTable {
  id?: string
  datasourceId?: string
  schemaName?: string
  tableName?: string
  tableComment?: string
  rowCount?: number
  dataSize?: number
  dataLevelCode?: string
  tags?: string
  status?: string
  lastSyncTime?: string
  checksum?: string
  lineageCount?: number
}

export interface MetaColumn {
  id?: string
  tableId?: string
  datasourceId?: string
  tableName?: string
  columnName?: string
  columnType?: string
  columnSize?: number
  nullable?: boolean
  defaultValue?: string
  comment?: string
  primaryKey?: boolean
  ordinalPosition?: number
  sensitiveFlag?: boolean
  dataLevelCode?: string
  status?: string
}

export interface MetaChange {
  id?: string
  datasourceId?: string
  tableName?: string
  changeType?: string
  checksum?: string
  syncedAt?: string
}

export const metaApi = {
  collect: (d: Record<string, unknown>) => apiPost<Record<string, unknown>>('/public/meta/collect', d),
  collectDs: (datasourceId: string) => apiPost<Record<string, unknown>>(`/public/meta/collect-ds/${datasourceId}`),
  collectAll: () => apiPost<Array<Record<string, unknown>>>('/public/meta/collect-all'),
  overview: () => apiGet<Record<string, unknown>>('/public/meta/overview'),
  tables: (datasourceId?: string) => apiGet<MetaTable[]>('/public/meta/tables', { datasourceId }),
  columns: (datasourceId?: string) => apiGet<MetaColumn[]>('/public/meta/columns', { datasourceId }),
  changes: (datasourceId?: string) => apiGet<MetaChange[]>('/public/meta/changes', { datasourceId }),
}

// ============ sys/preference (系统偏好) ============

export interface PreferenceEntity {
  id: string
  prefKey?: string
  prefValue?: string
  userId?: string
  createTime?: string
  updateTime?: string
}

export const preferenceApi = {
  all: () => apiGet<PreferenceEntity[]>('/preference'),
  save: (d: Partial<PreferenceEntity>) => apiPost<PreferenceEntity>('/preference', d),
  update: (d: Partial<PreferenceEntity>) => apiPut<PreferenceEntity>('/preference', d),
  remove: (id: string) => apiDelete<void>('/preference', { id }),
}

// ============ sys/view (视图管理) ============

export interface ViewEntity {
  id: string
  name?: string
  status?: string
  description?: string
}

export const sysViewApi = {
  list: () => apiGet<ViewEntity[]>('/view'),
  get: (id: string) => apiGet<ViewEntity>(`/view/${id}`),
  create: (d: Partial<ViewEntity>) => apiPost<ViewEntity>('/view', d),
  update: (id: string, d: Partial<ViewEntity>) => apiPut<ViewEntity>(`/view/${id}`, d),
  remove: (id: string) => apiDelete<void>(`/view/${id}`),
  publish: (id: string) => apiPost<ViewEntity>(`/view/${id}/publish`),
  execute: (id: string) => apiPost<string>(`/view/${id}/execute`),
}

// ============ sys/passwordless (免密登录) ============

export interface PasswordlessConfig {
  id: string
  loginType?: string
  config?: string
  status?: string
  target?: string
  code?: string
  createTime?: string
  updateTime?: string
}

export const passwordlessApi = {
  config: () => apiGet<PasswordlessConfig[]>('/passwordless/config'),
  saveConfig: (d: Partial<PasswordlessConfig>) =>
    apiPost<PasswordlessConfig>('/passwordless/config', d),
  send: (d: Record<string, string>) => apiPost<string>('/passwordless/send', d),
  verify: (d: Record<string, string>) => apiPost<boolean>('/passwordless/verify', d),
}