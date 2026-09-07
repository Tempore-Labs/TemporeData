/**
 * compute feature types + status tone mapping (v2.0).
 * JS-first (JSDoc) until the workspace adopts TypeScript (deferred infra step).
 */

/** Map container status -> StatusBadge tone. */
export const CONTAINER_TONE = {
  CREATED: 'info', RUNNING: 'running', STOPPED: 'stopped',
  FAILED: 'critical', UNKNOWN: 'unknown'
}

/** Map engine job status -> StatusBadge tone. */
export const ENGINE_TONE = {
  READY: 'info', RUNNING: 'running', SUCCESS: 'success',
  FAILED: 'danger', UNKNOWN: 'unknown'
}

/**
 * @typedef {Object} ComputeContainer
 * @property {string} id
 * @property {string} name
 * @property {string} type
 * @property {string} image
 * @property {string} status  CREATED/RUNNING/STOPPED/FAILED
 * @property {number} [cpuCores]
 * @property {number} [memoryMb]
 */

/**
 * @typedef {Object} EngineJob
 * @property {string} id
 * @property {string} name
 * @property {string} mainClass
 * @property {string} jarPath
 * @property {string} status  READY/RUNNING/SUCCESS/FAILED
 * @property {number} [parallelism]
 */