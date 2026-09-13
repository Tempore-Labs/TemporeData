/** Audit module API (P3-12 unified audit, hash-chain verified). */
import { apiGet, apiPost } from './http'
import type { PageQuery, PageResult } from '@/types/base'

export interface AuditEventEntity {
  id: string
  eventTime?: string
  tenantId?: string
  operator?: string
  ip?: string
  action?: string
  resourceType?: string
  resourceKey?: string
  detailJson?: string
  module?: string
  reqId?: string
  status?: string
  seq?: number
  prevHash?: string
  eventHash?: string
}

export interface AuditPolicyEntity {
  id: string
  module?: string
  action?: string
  auditLevel?: string
  retentionDays?: number
  notifyAlert?: boolean
}

export interface AuditArchiveEntity {
  id: string
  periodStart?: string
  periodEnd?: string
  recordCount?: number
  rootHash?: string
  signedBy?: string
  archivePath?: string
  createTime?: string
}

export const auditApi = {
  events: (module?: string, operator?: string) =>
    apiGet<AuditEventEntity[]>('/audit/events', { module, operator }),
  eventsPage: (params?: PageQuery) => apiGet<PageResult<AuditEventEntity>>('/audit/events/page', params),
  event: (id: string) => apiGet<Record<string, unknown>>(`/audit/events/${id}`),
  verifyRange: (from?: string, to?: string) =>
    apiPost<Record<string, unknown>>('/audit/verify/range', { from, to }),
  export: (from?: string, to?: string) => apiGet<Record<string, unknown>>('/audit/export', { from, to }),
  archives: () => apiGet<AuditArchiveEntity[]>('/audit/archives'),
  archive: () => apiPost<AuditArchiveEntity>('/audit/archive'),
  policies: () => apiGet<AuditPolicyEntity[]>('/audit/policies'),
  savePolicy: (p: Partial<AuditPolicyEntity>) => apiPost<AuditPolicyEntity>('/audit/policies', p),
}