/** Dataset & metadata module types (P0, /api/dataset, /api/metadata). */

export interface DatasetEntity {
  id: number
  name: string
  code: string
  domainId?: number
  layer?: string
  securityLevel?: string
  owner?: string
  createdAt?: string
  updatedAt?: string
}

export interface MetadataVersionEntity {
  id: number
  datasetId: number
  schemaJson: string
  driftType?: string
  createdAt?: string
}
