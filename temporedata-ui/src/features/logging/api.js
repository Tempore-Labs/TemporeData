/**
 * logging feature API — v2.0 migration placeholder.
 * The v1 backend /api/v1/logs is NOT ready yet, so these functions only document
 * the contract and bridge over services/http (baseURL /api/v1). No fake data is
 * served — until the contract lands the page resolves to error/empty state.
 * Contract reference: refactor/16-api-endpoint-matrix.md (log:read).
 */
import http from '../../services/http'

export const listLogs = (params) => http.get('/logs', { params })
export const getLogDetail = (id) => http.get(`/logs/${id}`)

// ---- Future /api/v1 contract (v2 backend) ----
// listLogs    = (params) => http.get('/logs', { params: { level, source, startTime, endTime, keyword, page, size } })
// getLogDetail = (id)   => http.get(`/logs/${id}`)