import { apiGet, apiPost, apiPut, apiDelete } from './http'
import type { PageQuery, PageResult } from '@/types/base'

export interface IngestionEntity {
  id: string
  name?: string
  type?: string
  datasourceId?: string
  targetTable?: string
  config?: string
  datasourceName?: string
  status?: string
  rowCount?: number
  lastRunTime?: string
  errorMsg?: string
  createTime?: string
}

export const ingestionApi = {
  list: () => apiGet<IngestionEntity[]>('/ingestion'),
  get: (id: string) => apiGet<IngestionEntity>(`/ingestion/${id}`),
  create: (d: Partial<IngestionEntity>) => apiPost<IngestionEntity>('/ingestion', d),
  update: (id: string, d: Partial<IngestionEntity>) => apiPut<IngestionEntity>(`/ingestion/${id}`, d),
  remove: (id: string) => apiDelete<void>(`/ingestion/${id}`),
  execute: (id: string) => apiPost<IngestionEntity>(`/ingestion/${id}/execute`),
}

export interface SyncEntity {
  id: string
  name?: string
  sourceDatasourceId?: string
  sourceTable?: string
  targetDatasourceId?: string
  targetTable?: string
  syncMode?: string
  incrementalColumn?: string
  incrementalValue?: string
  batchSize?: string
  config?: string
  status?: string
  rowCount?: number
  lastRunTime?: string
  duration?: string
  errorMsg?: string
  createTime?: string
}

export const syncApi = {
  list: () => apiGet<SyncEntity[]>('/sync'),
  get: (id: string) => apiGet<SyncEntity>(`/sync/${id}`),
  create: (d: Partial<SyncEntity>) => apiPost<SyncEntity>('/sync', d),
  update: (id: string, d: Partial<SyncEntity>) => apiPut<SyncEntity>(`/sync/${id}`, d),
  remove: (id: string) => apiDelete<void>(`/sync/${id}`),
  execute: (id: string) => apiPost<SyncEntity>(`/sync/${id}/execute`),
}

export interface EngineEntity {
  id: string
  name?: string
  type?: string
  jarPath?: string
  mainClass?: string
  sqlContent?: string
  pythonFile?: string
  flinkHome?: string
  parallelism?: string
  jobManagerMemory?: string
  taskManagerMemory?: string
  taskSlots?: number
  savepointPath?: string
  args?: string
  conf?: string
  status?: string
  jobId?: string
  lastRunTime?: string
  duration?: string
  errorMsg?: string
  createTime?: string
  master?: string
  deployMode?: string
  driverMemory?: string
  executorMemory?: string
  executorCores?: number
  numExecutors?: number
  appId?: string
}

export const engineApi = {
  sparkList: () => apiGet<EngineEntity[]>('/engine/spark'),
  flinkList: () => apiGet<EngineEntity[]>('/engine/flink'),
  sparkCreate: (d: Partial<EngineEntity>) => apiPost<EngineEntity>('/engine/spark', d),
  flinkCreate: (d: Partial<EngineEntity>) => apiPost<EngineEntity>('/engine/flink', d),
  sparkUpdate: (id: string, d: Partial<EngineEntity>) => apiPut<EngineEntity>(`/engine/spark/${id}`, d),
  flinkUpdate: (id: string, d: Partial<EngineEntity>) => apiPut<EngineEntity>(`/engine/flink/${id}`, d),
  remove: (id: string, type: string) => apiDelete<void>(`/engine/${type.toLowerCase()}/${id}`),
}

export interface OpsRepoEntity {
  id: string
  provider?: string
  repoRef?: string
  branch?: string
  autoTrigger?: boolean
  deployScriptPath?: string
  environment?: string
  enabled?: boolean
  bindInfoJson?: string
  createTime?: string
  updateTime?: string
}

export interface OpsBuildEntity {
  id: string
  repoId?: string
  triggerType?: string
  refName?: string
  commitSha?: string
  commitMessage?: string
  status?: string
  pipelineRef?: string
  artifactId?: string
  logRef?: string
  createTime?: string
  updateTime?: string
}

export const opsGitApi = {
  repos: () => apiGet<OpsRepoEntity[]>('/ops/repos'),
  bind: (d: Record<string, unknown>) => apiPost<OpsRepoEntity>('/ops/repos', d),
  unbind: (id: string) => apiDelete<void>(`/ops/repos/${id}`),
  syncRepo: (id: string) => apiPost<Record<string, unknown>>(`/ops/repos/${id}/sync`),
  trigger: (id: string) => apiPost<OpsBuildEntity>(`/ops/repos/${id}/trigger`),
  builds: (repoId?: string, status?: string) =>
    apiGet<OpsBuildEntity[]>('/ops/builds', { repoId, status }),
  providers: () => apiGet<Array<Record<string, unknown>>>(`/ops/providers`),
}

export interface MydataEntity {
  id: string
  userId?: string
  resourceType?: string
  resourceId?: string
  resourceName?: string
  expireTime?: string
  accessType?: string
  grantedBy?: string
  createTime?: string
}

export const mydataApi = {
  page: (params?: PageQuery) => apiGet<PageResult<MydataEntity>>('/mydata/page', params),
  list: () => apiGet<MydataEntity[]>('/mydata/list'),
  get: (id: string) => apiGet<MydataEntity>(`/mydata/${id}`),
  create: (d: Partial<MydataEntity>) => apiPost<MydataEntity>('/mydata', d),
  update: (d: Partial<MydataEntity>) => apiPut<MydataEntity>('/mydata', d),
  remove: (id: string) => apiDelete<void>(`/mydata/${id}`),
}

export interface DatacenterEntity {
  id: string
  name?: string
  description?: string
  category?: string
  config?: string
  datasourceId?: string
  refreshInterval?: number
  status?: string
  datasourceCount?: number
  tableCount?: number
  workflowCount?: number
  apiCount?: number
  syncTaskCount?: number
  qualityRuleCount?: number
  createTime?: string
  updateTime?: string
}

export const datacenterApi = {
  page: (params?: PageQuery) => apiGet<PageResult<DatacenterEntity>>('/datacenter/page', params),
  list: () => apiGet<DatacenterEntity[]>('/datacenter/list'),
  get: (id: string) => apiGet<DatacenterEntity>(`/datacenter/${id}`),
  create: (d: Partial<DatacenterEntity>) => apiPost<DatacenterEntity>('/datacenter', d),
  update: (d: Partial<DatacenterEntity>) => apiPut<DatacenterEntity>('/datacenter', d),
  remove: (id: string) => apiDelete<void>(`/datacenter/${id}`),
}

export interface PermapprovalEntity {
  id: string
  resourceType?: string
  resourceId?: string
  resourceName?: string
  accessType?: string
  reason?: string
  expireTime?: string
  applicantId?: string
  applicantName?: string
  status?: string
  approverId?: string
  approverName?: string
  approveComment?: string
  createTime?: string
  updateTime?: string
}

export const permapprovalApi = {
  page: (params?: PageQuery) => apiGet<PageResult<PermapprovalEntity>>('/permapproval/page', params),
  list: () => apiGet<PermapprovalEntity[]>('/permapproval/list'),
  pending: () => apiGet<PermapprovalEntity[]>('/permapproval/pending'),
  my: (applicantId?: string) => apiGet<PermapprovalEntity[]>('/permapproval/my', { applicantId }),
  get: (id: string) => apiGet<PermapprovalEntity>(`/permapproval/${id}`),
  create: (d: Partial<PermapprovalEntity>) => apiPost<PermapprovalEntity>('/permapproval', d),
  update: (id: string, d: Partial<PermapprovalEntity>) => apiPut<PermapprovalEntity>(`/permapproval/${id}`, d),
  approve: (id: string) => apiPost<PermapprovalEntity>(`/permapproval/${id}/approve`),
  reject: (id: string) => apiPost<PermapprovalEntity>(`/permapproval/${id}/reject`),
  remove: (id: string) => apiDelete<void>(`/permapproval/${id}`),
}

export interface ApilogEntity {
  id: string
  apiName?: string
  apiPath?: string
  method?: string
  requestIp?: string
  requestParams?: string
  responseCode?: number
  responseBody?: string
  costTime?: number
  callerId?: string
  createTime?: string
}

export const apilogApi = {
  page: (params?: PageQuery) => apiGet<PageResult<ApilogEntity>>('/apilog/page', params),
  list: () => apiGet<ApilogEntity[]>('/apilog/list'),
  get: (id: string) => apiGet<ApilogEntity>(`/apilog/${id}`),
  create: (d: Partial<ApilogEntity>) => apiPost<ApilogEntity>('/apilog', d),
  update: (d: Partial<ApilogEntity>) => apiPut<ApilogEntity>('/apilog', d),
  remove: (id: string) => apiDelete<void>(`/apilog/${id}`),
}

export interface FormEntity {
  id: string
  name?: string
  description?: string
  config?: string
  status?: string
  shareToken?: string
  createTime?: string
  updateTime?: string
  formId?: string
  data?: string
  submitterIp?: string
  submitTime?: string
}

export const formApi = {
  list: () => apiGet<FormEntity[]>('/form/list'),
  create: (d: Partial<FormEntity>) => apiPost<FormEntity>('/form/create', d),
  update: (id: string, d: Partial<FormEntity>) => apiPut<FormEntity>(`/form/${id}`, d),
  remove: (id: string) => apiDelete<void>(`/form/${id}`),
  submissions: (id: string) => apiGet<Array<Record<string, unknown>>>(`/form/${id}/submissions`),
  shareToken: (id: string) => apiPost<string>(`/form/${id}/share-token`),
}

export interface FuncEntity {
  id: string
  name?: string
  type?: string
  language?: string
  script?: string
  description?: string
  status?: string
  createTime?: string
}

export const funcApi = {
  page: (params?: PageQuery) => apiGet<PageResult<FuncEntity>>('/func/page', params),
  list: () => apiGet<FuncEntity[]>('/func/list'),
  get: (id: string) => apiGet<FuncEntity>(`/func/${id}`),
  create: (d: Partial<FuncEntity>) => apiPost<FuncEntity>('/func', d),
  update: (d: Partial<FuncEntity>) => apiPut<FuncEntity>('/func', d),
  remove: (id: string) => apiDelete<void>(`/func/${id}`),
}

export interface GlobalvarEntity {
  id: string
  name?: string
  status?: string
  description?: string
}

export const globalvarApi = {
  page: (params?: PageQuery) => apiGet<PageResult<GlobalvarEntity>>('/globalvar/page', params),
  list: () => apiGet<GlobalvarEntity[]>('/globalvar/list'),
  get: (id: string) => apiGet<GlobalvarEntity>(`/globalvar/${id}`),
  create: (d: Partial<GlobalvarEntity>) => apiPost<GlobalvarEntity>('/globalvar', d),
  update: (d: Partial<GlobalvarEntity>) => apiPut<GlobalvarEntity>('/globalvar', d),
  remove: (id: string) => apiDelete<void>(`/globalvar/${id}`),
}

export interface DependencyEntity {
  id: string
  name?: string
  status?: string
  description?: string
}

export const dependencyApi = {
  page: (params?: PageQuery) => apiGet<PageResult<DependencyEntity>>('/dependency/page', params),
  list: () => apiGet<DependencyEntity[]>('/dependency/list'),
  get: (id: string) => apiGet<DependencyEntity>(`/dependency/${id}`),
  create: (d: Partial<DependencyEntity>) => apiPost<DependencyEntity>('/dependency', d),
  update: (d: Partial<DependencyEntity>) => apiPut<DependencyEntity>('/dependency', d),
  remove: (id: string) => apiDelete<void>(`/dependency/${id}`),
}