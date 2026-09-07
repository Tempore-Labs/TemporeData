/**
 * incidents feature types + status tone mapping (v2.0).
 * JS-first (JSDoc) until the workspace adopts TypeScript (deferred infra step).
 */

/** Map incident lifecycle status -> StatusBadge tone. */
export const INCIDENT_TONE = {
  NEW: 'critical', OPEN: 'warning', SENT: 'info', PENDING: 'info',
  ACKNOWLEDGED: 'primary', RESOLVED: 'success', CLOSED: 'success', UNKNOWN: 'unknown'
}

/** Map incident severity/level -> StatusBadge tone. */
export const INCIDENT_LEVEL_TONE = { CRITICAL: 'critical', WARNING: 'warning', INFO: 'info', UNKNOWN: 'info' }

/**
 * @typedef {Object} Incident
 * @property {string} id
 * @property {string} [baselineId]
 * @property {string} eventType
 * @property {string} message
 * @property {string} level
 * @property {string} status  NEW/ACKNOWLEDGED/RESOLVED/SENT...
 * @property {string} [createTime]
 */