/** Incident module types (P2, /api/incident). */

export interface IncidentEntity {
  id: number
  title: string
  sourceType?: string
  datasetId?: number
  severity?: string
  status?: string
  rcaAnalysis?: string
  createdAt?: string
  updatedAt?: string
}
