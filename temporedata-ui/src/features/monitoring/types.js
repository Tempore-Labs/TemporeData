/**
 * monitoring feature types + status tone mapping (v2.0).
 * JS-first (JSDoc) until the workspace adopts TypeScript (deferred infra step).
 */

/** Map instance/execution state -> StatusBadge tone. */
export const METRIC_TONE = {
  RUNNING: 'running', SUCCESS: 'success', FAILED: 'danger',
  PENDING: 'info', QUEUED: 'primary', CANCELLED: 'info', UNKNOWN: 'unknown'
}

/**
 * @typedef {Object} SystemOverview
 * @property {{active:number,total:number}} cluster  cluster health counts
 * @property {{active:number,total:number}} datasource  datasource counts
 * @property {{active:number,total:number}} workflow  workflow counts
 * @property {{success:number,total:number}} execution  execution counts
 */

/**
 * @typedef {Object} InstanceStats
 * @property {number} total
 * @property {number} running
 * @property {number} success
 * @property {number} failed
 */

/**
 * @typedef {Object} ClusterMetricRow
 * @property {string} time
 * @property {number} cpuPercent
 * @property {number} usedMemorySize
 * @property {number} diskIoReadSpeed
 * @property {number} networkIoReadSpeed
 */