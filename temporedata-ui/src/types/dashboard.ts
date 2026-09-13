/** Dashboard module types (GET /api/dashboard). */

export interface StatItem {
  name: string
  value: number
}

export interface TableStat {
  tableName: string
  rowCount: number
}

export interface SyncStat {
  taskName: string
  status: string
  lastRunTime: string
  rowCount: number
}

export interface TrendItem {
  hour: string
  success: number
  failed: number
}

export interface FailedTask {
  taskName: string
  reason: string
  lastRunTime: string
}

export interface FailRule {
  ruleName: string
  datasetName?: string
  score: number
}

export interface DataSourceStat {
  name: string
  type: string
}

export interface DashboardRes {
  datasourceCount: number
  tableCount: number
  totalRows: number
  syncTaskCount: number
  syncSuccessCount: number
  syncFailCount: number
  qualityRuleCount: number
  qualityPassCount: number
  qualityFailCount: number
  workflowCount: number
  apiCount: number
  apiCallTotal: number
  syncStatus: StatItem[]
  qualityStatus: StatItem[]
  topTables: TableStat[]
  recentSyncs: SyncStat[]
  hourlyTrend: TrendItem[]
  datasourceTypeDist: StatItem[]
  failedSyncs: FailedTask[]
  failedRules: FailRule[]
  datasourceList?: DataSourceStat[]
}
