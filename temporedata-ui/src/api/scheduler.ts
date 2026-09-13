/** Scheduler API (P0-1). */
import { apiDelete, apiGet, apiPost, apiPut } from './http'

export interface TaskDefineRes {
  id: string
  name: string
  taskType?: string
  targetRef?: string
  cronExpression?: string
  bizDateMode?: string
  calendarId?: string
  timezone?: string
  enabled?: boolean
  params?: string
  owner?: string
  status?: string
  createTime?: string
  updateTime?: string
}

export interface TaskDefineReq {
  name: string
  taskType?: string
  targetRef?: string
  cronExpression?: string
  bizDateMode?: string
  calendarId?: string
  timezone?: string
  enabled?: boolean
  params?: string
  owner?: string
}

/** One business date in a backfill / preview range. */
export interface BizDateItem {
  bizDate: string
  workday: boolean
  triggerTime: string
}

/** A single execution log line for a task instance. */
export interface TaskLogItem {
  id: string
  instanceId?: string
  level?: string
  message?: string
  createTime?: string
}

export const schedulerApi = {
  tasks: () => apiGet<TaskDefineRes[]>('/scheduler/tasks'),
  task: (id: string) => apiGet<TaskDefineRes>(`/scheduler/tasks/${id}`),
  create: (req: TaskDefineReq) => apiPost<TaskDefineRes>('/scheduler/tasks', req),
  update: (id: string, req: TaskDefineReq) => apiPut<TaskDefineRes>(`/scheduler/tasks/${id}`, req),
  remove: (id: string) => apiDelete<void>(`/scheduler/tasks/${id}`),
  setEnabled: (id: string, enabled: boolean) =>
    apiPut<TaskDefineRes>(`/scheduler/tasks/${id}/enabled`, { enabled }),
  trigger: (id: string) => apiPost<any>(`/scheduler/tasks/${id}/trigger`),
  instances: (id: string) => apiGet<any[]>(`/scheduler/tasks/${id}/instances`),
  logs: (instanceId: string) => apiGet<TaskLogItem[]>(`/scheduler/instances/${instanceId}/logs`),
  bizDates: (id: string, from: string, to: string) =>
    apiGet<BizDateItem[]>(`/scheduler/tasks/${id}/bizdates`, { from, to }),
  backfill: (id: string, from: string, to: string) =>
    apiPost<number>(`/scheduler/tasks/${id}/backfill`, { from, to }, true),
}
