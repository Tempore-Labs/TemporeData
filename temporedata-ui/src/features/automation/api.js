/**
 * automation feature API — v2.0 migration placeholder.
 * The v1 backend /api/v1/automation is NOT ready yet; these functions document the
 * contract and bridge over services/http (baseURL /api/v1). No fake data is
 * served — until the contract lands the pages resolve to error/empty state.
 * Runbook execution is a HIGH-RISK write: never invoked directly from a list page;
 * gate with canExecute('automation:item:execute') and route through action preview.
 * Contract reference: refactor/16-api-endpoint-matrix.md (automation:read / write).
 */
import http from '../../services/http'

export const listRunbooks = (params) => http.get('/automation/runbooks', { params })
export const getRunbook = (id) => http.get(`/automation/runbooks/${id}`)
// High-risk write. Confirm via action preview before calling.
export const executeRunbook = (id) => http.post(`/automation/runbooks/${id}/execute`)
export const listExecutions = (params) => http.get('/automation/executions', { params })
export const getExecution = (id) => http.get(`/automation/executions/${id}`)