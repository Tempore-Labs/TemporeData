/** Asset module types (P0, /api/asset). */

export interface AssetCatalogEntity {
  id: number
  datasetId: number
  businessTerm?: string
  tags?: string
  popularity?: number
  owner?: string
  createdAt?: string
  updatedAt?: string
}

export interface MetricEntity {
  id: number
  code: string
  name: string
  metricType?: string
  datasetId?: number
  definitionSql?: string
  owner?: string
  createdAt?: string
  updatedAt?: string
}
