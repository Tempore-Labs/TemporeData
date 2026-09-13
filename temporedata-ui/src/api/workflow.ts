/** Workflow API (DAG) — DTO types strictly aligned with backend
 *  `org.temporedata.api.dev.workflow.*`. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'

export type WorkflowNodeType =
  | 'SQL'
  | 'SHELL'
  | 'PYTHON'
  | 'FLINK'
  | 'SPARK'
  | 'HTTP'
  | 'QUALITY'
export type WorkflowEdgeType = 'SUCCESS' | 'FAILURE' | 'ALWAYS'
export type NodeRunStatus = 'WAITING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED' | 'PENDING'

// ---------- nodes / edges (transport layer, aligned to WorkflowNodeRes/WorkflowEdgeRes) ----------

export interface WorkflowNodeRes {
  id: string
  name: string
  type: WorkflowNodeType
  datasourceId?: string
  datasourceName?: string
  sql?: string
  positionX?: number
  positionY?: number
  retryCount?: number
  retryInterval?: number
  timeoutSeconds?: number
  priority?: string
  failStrategy?: string
  params?: string
  sparkConf?: string
  httpUrl?: string
  httpMethod?: string
  httpHeaders?: string
  dependencyType?: string
  dependencyTimeout?: number
  emailTo?: string
  emailSubject?: string
  qualityType?: string
  qualityThreshold?: number
}

export type WorkflowNodeDTO = WorkflowNodeRes

export interface WorkflowEdgeRes {
  id: string
  sourceNodeId: string
  targetNodeId: string
  edgeType?: WorkflowEdgeType
  conditionExpr?: string
}

export type WorkflowEdgeDTO = WorkflowEdgeRes

// ---------- workflow ----------

export interface WorkflowRes {
  id: string
  name: string
  description?: string
  status?: string
  cronExpression?: string
  schedulePolicy?: string
  scheduleMissfire?: string
  nodes?: WorkflowNodeDTO[]
  edges?: WorkflowEdgeDTO[]
  createTime?: string
}

export type WorkflowReq = Pick<WorkflowRes, 'name' | 'description'> & {
  nodes: WorkflowNodeDTO[]
  edges: WorkflowEdgeDTO[]
}

// ---------- execution / instances ----------

export interface NodeRunItem {
  nodeId: string
  nodeName: string
  status: string
  errorMsg?: string
  durationMs: number
}

export interface WorkflowRunRes {
  instanceId: string
  status: string
  startTime?: string
  endTime?: string
  nodeResults?: NodeRunItem[]
}

export interface WorkflowNodeInstanceRes {
  id: string
  nodeId: string
  nodeName: string
  nodeType?: string
  status: NodeRunStatus
  startTime?: string
  finishTime?: string
  durationMs?: number
  retryTimes?: number
  result?: string
  errorMsg?: string
}

export interface WorkflowInstanceRes {
  id: string
  workflowId: string
  taskInstanceId?: string
  status: string
  triggerType?: string
  startTime?: string
  finishTime?: string
  resultMsg?: string
  nodes?: WorkflowNodeInstanceRes[]
}

export interface InstanceLogItem {
  id: string
  nodeInstanceId?: string
  level?: string
  message?: string
  createTime?: string
}

export interface RuntimeCommand {
  id: string
  instanceId: string
  type?: string
  scope?: string
  targetNodeId?: string
  state?: string
  operator?: string
  reason?: string
  createTime?: string
  doneTime?: string
}

export interface WorkflowVersion {
  id: string
  workflowId?: string
  versionNo?: number
  name?: string
  nodesJson?: string
  edgesJson?: string
  remark?: string
  createTime?: string
}

export interface ColumnLineage {
  targetTable?: string
  targetColumn?: string
  sourceTable?: string
  sourceColumn?: string
}

export interface SqlParseRes {
  sqlType?: string
  targetTable?: string
  sources?: string[]
  tables?: string[]
  columns?: string[]
  columnLineage?: ColumnLineage[]
  success?: boolean
  message?: string
}

export const workflowApi = {
  // definition
  list: () => apiGet<WorkflowRes[]>('/workflow'),
  get: (id: string) => apiGet<WorkflowRes>(`/workflow/${id}`),
  create: (req: WorkflowReq) => apiPost<WorkflowRes>('/workflow', req),
  update: (id: string, req: WorkflowReq) => apiPut<WorkflowRes>(`/workflow/${id}`, req),
  remove: (id: string) => apiDelete<void>(`/workflow/${id}`),
  online: (id: string) => apiPost<WorkflowRes>(`/workflow/${id}/online`),
  offline: (id: string) => apiPost<WorkflowRes>(`/workflow/${id}/offline`),
  versions: (id: string) => apiGet<WorkflowVersion[]>(`/workflow/${id}/versions`),
  rollback: (id: string, versionId: string) =>
    apiPost<WorkflowRes>(`/workflow/${id}/versions/${versionId}/rollback`),
  sqlLineageParse: (sql: string, datasourceType?: string) =>
    apiPost<SqlParseRes>('/workflow-lineage/parse', { sql, datasourceType }),
  schedule: (id: string, cronExpression: string, enabled: boolean, opts?: {
    schedulePolicy?: string
    scheduleMissfire?: string
  }) =>
    apiPut<WorkflowRes>(`/workflow/${id}/schedule`, {
      cronExpression,
      enabled,
      schedulePolicy: opts?.schedulePolicy,
      scheduleMissfire: opts?.scheduleMissfire,
    }),

  // execution
  run: (id: string) => apiPost<WorkflowRunRes>(`/workflow/${id}/run`),
  execute: (id: string) => apiPost<{ status?: string }>(`/workflow/${id}/execute`),
  backfill: (id: string, body: {
    start: string
    end: string
    interval?: string
    concurrency?: string
  }) => apiPost<{ generated: number; dates: string[]; interval: string; concurrency: string }>(
    `/workflow/${id}/backfill`,
    body,
  ),
  instances: (id: string) => apiGet<WorkflowInstanceRes[]>(`/workflow/${id}/instances`),
  instanceDetail: (instanceId: string) =>
    apiGet<WorkflowInstanceRes>(`/workflow/instances/${instanceId}`),
  logs: (instanceId: string) =>
    apiGet<InstanceLogItem[]>(`/workflow/instances/${instanceId}/logs`),

  // runtime control (P0-3)
  pause: (instanceId: string) => apiPost<void>(`/workflow/instances/${instanceId}/pause`),
  resume: (instanceId: string) => apiPost<void>(`/workflow/instances/${instanceId}/resume`),
  stop: (instanceId: string) => apiPost<void>(`/workflow/instances/${instanceId}/stop`),
  forceSuccess: (instanceId: string, nodeInstanceId: string) =>
    apiPost<void>(`/workflow/instances/${instanceId}/force-success`, { nodeInstanceId }),
  recoverFailed: (instanceId: string) =>
    apiPost<WorkflowRunRes>(`/workflow/instances/${instanceId}/recover-failed`),
  rerun: (instanceId: string, scope?: string, fromNodeId?: string) =>
    apiPost<WorkflowRunRes>(`/workflow/instances/${instanceId}/rerun`, { scope, fromNodeId }),
  setPriority: (instanceId: string, priority: number) =>
    apiPut<void>(`/workflow/instances/${instanceId}/priority`, { priority }),
  setPool: (instanceId: string, pool: string) =>
    apiPut<void>(`/workflow/instances/${instanceId}/pool`, { pool }),
  commands: (instanceId: string) =>
    apiGet<RuntimeCommand[]>(`/workflow/instances/${instanceId}/commands`),
}