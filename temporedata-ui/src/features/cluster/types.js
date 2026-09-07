/**
 * cluster feature types + status tone mapping (v2.0).
 * JS-first (JSDoc) until the workspace adopts TypeScript (deferred infra step).
 */

/** Map backend status -> StatusBadge tone (healthy/running/warning/critical/stopped/unknown/maintenance). */
export const CLUSTER_TONE = {
  ACTIVE: 'healthy', RUNNING: 'running', PENDING: 'info', DOWN: 'critical',
  STOPPED: 'stopped', MAINTENANCE: 'maintenance', UNKNOWN: 'unknown'
}

/**
 * @typedef {Object} ClusterRow
 * @property {string} id
 * @property {string} name
 * @property {string} type
 * @property {string} masterUrl
 * @property {string} status
 * @property {number} [nodeCount]
 */

/**
 * @typedef {Object} ClusterNode
 * @property {string} id
 * @property {string} host
 * @property {number} port
 * @property {string} agentStatus
 */