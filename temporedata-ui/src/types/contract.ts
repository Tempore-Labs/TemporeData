/** Contract module types (P1, /api/contract). */

export interface ContractEntity {
  id: number
  datasetId: number
  contractYaml: string
  compatibilityMode?: string
  status?: string
  owner?: string
  createdAt?: string
  updatedAt?: string
}

export interface ContractCheckResult {
  compatible: boolean
  changes: string[]
}
