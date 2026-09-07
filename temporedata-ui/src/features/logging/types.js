/**
 * logging feature types + status tone mapping (v2.0).
 * JS-first (JSDoc) until the workspace adopts TypeScript (deferred infra step).
 */

/** Map log level -> StatusBadge tone. */
export const LOG_LEVEL_TONE = {
  ERROR: 'critical', WARN: 'warning', WARNING: 'warning',
  INFO: 'info', DEBUG: 'info', TRACE: 'info', UNKNOWN: 'info'
}

/**
 * @typedef {Object} LogRow
 * @property {string} id
 * @property {string} [timestamp]
 * @property {string} level  ERROR/WARN/INFO/DEBUG/TRACE
 * @property {string} [source]
 * @property {string} [message]
 */

/**
 * @typedef {Object} LogQuery
 * @property {string} [level]
 * @property {string} [source]
 * @property {string} [keyword]
 * @property {string} [startTime]
 * @property {string} [endTime]
 */