/**
 * data feature types + status tone mapping (v2.0).
 * JS-first (JSDoc) until the workspace adopts TypeScript (deferred infra step).
 */

/** Map datasource status -> StatusBadge tone. */
export const DATASOURCE_TONE = {
  CONNECTED: 'healthy', ACTIVE: 'healthy', DISCONNECTED: 'critical',
  INACTIVE: 'stopped', UNKNOWN: 'unknown'
}

/**
 * @typedef {Object} DatasourceRow
 * @property {string} id
 * @property {string} name
 * @property {string} type
 * @property {string} host
 * @property {number} port
 * @property {string} status  CONNECTED/DISCONNECTED/ACTIVE/INACTIVE
 */

/**
 * @typedef {Object} MetaOverview
 * @property {number} [totalTables]
 * @property {number} [totalColumns]
 * @property {number} [collectedCount]
 * @property {number} [datasourceCount]
 */