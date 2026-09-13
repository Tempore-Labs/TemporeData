/** Quality module types (P0, /api/quality). */

export interface QualityRuleEntity {
  id: number
  datasetId: number
  dimension?: string
  ruleType?: string
  expression?: string
  thresholdScore?: number
  isBlocking?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface QualityGateEntity {
  id: number
  datasetId: number
  ruleId?: number
  minScore?: number
  status?: string
  blocking?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface QualityAssessResult {
  score: number
  passed: boolean
}
