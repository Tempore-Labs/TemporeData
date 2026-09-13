/** RCA-Agent API. */
import { apiPost } from './http'
import type { ActionPlan, ApplyOutcome } from '@/types/rca'

export const rcaApi = {
  analyze: (incidentId: number) => apiPost<ActionPlan>(`/rca/${incidentId}/analyze`),
  apply: (incidentId: number, approve: boolean) =>
    apiPost<ApplyOutcome>(`/rca/${incidentId}/apply`, { approve }, true),
}
