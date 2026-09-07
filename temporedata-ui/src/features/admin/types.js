/**
 * admin feature types + status tone mapping (v2.0).
 * JS-first (JSDoc) until the workspace adopts TypeScript (deferred infra step).
 */

/** Map admin entity status -> StatusBadge tone (e.g. tenant status). */
export const ADMIN_TONE = {
  ACTIVE: 'healthy', ENABLED: 'healthy', DISABLED: 'stopped', UNKNOWN: 'unknown'
}

/**
 * @typedef {Object} TenantRow
 * @property {string} id
 * @property {string} name
 * @property {string} status  ACTIVE/ENABLED/DISABLED
 * @property {string} [createTime]
 */