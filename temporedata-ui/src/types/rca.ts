/** RCA-Agent types (P2, /api/rca). */

export interface RcaFinding {
  incidentId: number
  causeType: string
  hypothesis: string
  confidence: number
  at: string
}

export interface ActionItem {
  title: string
  suggestedCommand: string
  actionType: string
  allowed: boolean
  requiresApproval: boolean
  risk: string
  reason: string
}

export interface ActionPlan {
  incidentId: number
  finding: RcaFinding
  actions: ActionItem[]
  at: string
}

export interface ApplyOutcome {
  incidentId: number
  status: string
  applied: number
  needsApproval: number
  executed: string[]
}
