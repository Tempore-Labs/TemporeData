export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  traceId: string
}

export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
}

export interface Job {
  id: string
  name: string
  type: string
  status: 'DRAFT' | 'ENABLED' | 'DISABLED'
}

export interface JobExecution {
  id: string
  jobId: string
  status: 'QUEUED' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'
  executorType: string
  traceId?: string
}

export interface ActionPlan {
  id: string
  incidentId?: string
  actionType: string
  targetId: string
  risk: 'L0' | 'L1' | 'L2' | 'L3' | 'L4'
  reason: string
  evidence: Array<{ type: string; ref: string }>
}
