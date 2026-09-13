/** Alarm module API: /api/alarm/config (AlarmEntity) + /api/alarm (Baseline/Record). */
import { apiDelete, apiGet, apiPost, apiPut } from './http'

export interface AlarmEntity {
  id: string
  name: string
  eventType?: string
  channels?: string
  email?: string
  webhookUrl?: string
  enabled?: boolean
  message?: string
  status?: string
  sendTime?: string
  createTime?: string
}

export interface AlarmBaselineEntity {
  id: string
  name: string
  workflowId: string
  calendarId?: string
  bizDateMode?: string
  timezone?: string
  expectTime: string
  triggerType?: string
  graceSeconds?: number
  enabled: boolean
  notifyChannels?: string
  operator?: string
  remark?: string
  createTime?: string
}

export interface AlarmRecordEntity {
  id: string
  baselineId: string
  wfInstanceId?: string
  bizDate?: string
  alarmType: string
  expectFinishTime?: string
  actualFinishTime?: string
  content?: string
  status: string
  createTime?: string
  ackBy?: string
  ackTime?: string
  closeTime?: string
}

export const alarmApi = {
  // config (rule)
  listConfig: () => apiGet<AlarmEntity[]>('/alarm/config'),
  createConfig: (d: Partial<AlarmEntity>) => apiPost<AlarmEntity>('/alarm/config', d),
  updateConfig: (id: string, d: Partial<AlarmEntity>) => apiPut<AlarmEntity>(`/alarm/config/${id}`, d),
  removeConfig: (id: string) => apiDelete<void>(`/alarm/config/${id}`),
  configRecords: (id: string) => apiGet<Record<string, unknown>[]>(`/alarm/config/${id}/records`),
  testConfig: (id: string) => apiPost<Record<string, unknown>>(`/alarm/config/${id}/test`),

  // baselines
  listBaselines: () => apiGet<AlarmBaselineEntity[]>('/alarm/baselines'),
  createBaseline: (d: Partial<AlarmBaselineEntity>) => apiPost<AlarmBaselineEntity>('/alarm/baselines', d),
  updateBaseline: (id: string, d: Partial<AlarmBaselineEntity>) => apiPut<AlarmBaselineEntity>(`/alarm/baselines/${id}`, d),
  removeBaseline: (id: string) => apiDelete<void>(`/alarm/baselines/${id}`),
  toggleBaseline: (id: string, enabled: boolean) =>
    apiPost<AlarmBaselineEntity>(`/alarm/baselines/${id}/enable`, { enabled }, true),
  testBaseline: (id: string) => apiPost<string>(`/alarm/baselines/${id}/test`),

  // records
  records: (baselineId?: string, status?: string) =>
    apiGet<AlarmRecordEntity[]>('/alarm/records', { baselineId, status }),
  ackRecord: (id: string) => apiPost<AlarmRecordEntity>(`/alarm/records/${id}/ack`),
  closeRecord: (id: string) => apiPost<AlarmRecordEntity>(`/alarm/records/${id}/close`),
  runCheck: () => apiPost<Record<string, unknown>>('/alarm/run-check'),
}