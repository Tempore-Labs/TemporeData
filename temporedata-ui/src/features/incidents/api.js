/**
 * incidents feature API — v2.0 migration.
 * Short-term: bridges the existing alarm backend (baselineApi records = triggered
 * incidents, ops/alarm -> service/incident per refactor/12-source-migration-map.md)
 * so the migrated pages keep working today. Swaps to /api/v1/incidents when the
 * contract lands (refactor/16-api-endpoint-matrix.md: incident:read / operate).
 */
import { baselineApi } from '@/api/modules/alarm'

export const listIncidents = (params) => baselineApi.records(params?.baselineId, params?.status)
export const getIncident = async (id) => {
  const records = await baselineApi.records()
  return (records || []).find((r) => r.id === id) || null
}
export const ackIncident = (id) => baselineApi.ack(id)
export const resolveIncident = (id) => baselineApi.close(id)

// ---- Future /api/v1 contract (v2 backend) ----
// listIncidents   = (params) => http.get('/incidents', { params })
// getIncident     = (id) => http.get(`/incidents/${id}`)
// ackIncident     = (id) => http.post(`/incidents/${id}/ack`)
// resolveIncident = (id) => http.post(`/incidents/${id}/resolve`)