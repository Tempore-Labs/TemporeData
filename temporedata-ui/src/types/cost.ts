/** Cost module types (P2, /api/cost). */

export interface CostEntity {
  id: number
  datasetId: number
  computeCost?: number
  storageCost?: number
  queryCost?: number
  recordDate: string
  createdAt?: string
}

export interface CostSummary {
  datasetId: number
  from: string
  to: string
  computeCost: number
  storageCost: number
  queryCost: number
  dayCount: number
}
