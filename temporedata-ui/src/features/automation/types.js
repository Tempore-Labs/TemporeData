/**
 * automation feature types + status tone mapping (v2.0).
 * JS-first (JSDoc) until the workspace adopts TypeScript (deferred infra step).
 */

/** Map runbook state -> StatusBadge tone. */
export const RUNBOOK_TONE = {
  DRAFT: 'info', ENABLED: 'healthy', DISABLED: 'stopped', ARCHIVED: 'info', UNKNOWN: 'unknown'
}

/** Map execution status -> StatusBadge tone. */
export const EXECUTION_TONE = {
  QUEUED: 'info', RUNNING: 'running', SUCCESS: 'success',
  FAILED: 'danger', CANCELLED: 'info', UNKNOWN: 'unknown'
}

/**
 * @typedef {Object} Runbook
 * @property {string} id
 * @property {string} name
 * @property {string} [description]
 * @property {string} status  DRAFT/ENABLED/DISABLED/ARCHIVED
 * @property {string} [triggerType]
 * @property {string} [createTime]
 */

/**
 * @typedef {Object} AutomationExecution
 * @property {string} id
 * @property {string} [runbookId]
 * @property {string} [runbookName]
 * @property {string} status  QUEUED/RUNNING/SUCCESS/FAILED/CANCELLED
 * @property {string} [triggeredBy]
 * @property {string} [startTime]
 * @property {string} [endTime]
 */