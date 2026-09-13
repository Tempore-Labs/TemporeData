/** AI safety gate types (P2, /api/ai-safety). */

export type AiActionType =
  | 'QUERY'
  | 'EXPORT'
  | 'WRITE'
  | 'DROP'
  | 'ALTER'
  | 'GRANT'
  | 'OTHER'

export type AiRisk = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface AiCommandRequest {
  type?: AiActionType
  principal?: string
  targetDatasetId?: number
  target?: string
  securityLevel?: string
  command?: string
  autoApproved?: boolean
}

export interface AiPolicyDecision {
  allowed: boolean
  risk: AiRisk
  reason: string
  requiresApproval: boolean
}
