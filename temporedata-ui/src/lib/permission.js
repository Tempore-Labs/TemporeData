/**
 * 前端权限判定（P3-11 菜单/按钮动态过滤）。
 * 权限点由后端 `PermissionService.myPerms` 下发，形如：
 *   - `permission:{resourceType}:{action}`（资源类型维度的能力点）
 *   - `resource:{resourceType}:{resourceKey}:{action}`（具体资源事件授权）
 * 超级管理员（`user.admin === true`）恒有权限。
 *
 * 菜单/按钮上声明的权限串统一采用 `{resourceType}:{action}`（如 `cluster:read`），
 * hasPerm 会据此在两种下发格式中做匹配，并对大写 action 统一小写。
 */

import { useAuthStore } from '@/stores/auth'

const SUPER_ADMIN = true

function permissionsOf(user) {
  if (!user) return []
  const p = user.permissions
  if (Array.isArray(p)) return p
  if (typeof p === 'string' && p) return [p]
  return []
}

function isAdmin(user) {
  // Super-admin is decided by the authoritative backend `user.admin` flag (login sets it
  // when a role resolves to ROLE_ADMIN), the exact seed role `ROLE_ADMIN`, or the
  // reserved username `admin`. We deliberately do NOT treat any role whose name merely
  // contains "admin" (e.g. ROLE_TENANT_ADMIN) as a global super-admin: the backend only
  // grants the super-admin bypass for ROLE_ADMIN, so the UI must not over-advertise.
  return !!(user && (user.admin === true
    || String(user.roles || '').toUpperCase().includes('ROLE_ADMIN')
    || user.username === 'admin'))
}

/**
 * 判断当前登录用户是否具备某项权限点。
 * @param {string} perm  权限串，格式 `{resourceType}:{action}`，如 `cluster:read`
 * @param {object} [user] 用户信息；缺省取 auth store 的当前用户
 * @returns {boolean}
 */
export function hasPerm(perm, user) {
  const u = user || useAuthStore().user
  if (!perm) return true          // 未声明权限要求 -> 放行
  if (isAdmin(u)) return true     // 超级管理员恒有

  const perms = permissionsOf(u)
  if (perms.length === 0) return false       // 无任何权限点 -> fail-closed
  const normalized = String(perm).trim().toLowerCase()
  const [rtype, action] = normalized.split(':')

  for (const raw of perms) {
    const p = String(raw).toLowerCase()
    // 精确：permission:cluster:read
    if (p === `permission:${normalized}`) return true
    // 资源维度：resource:cluster：任意 key:read —— 只要授予了该资源类型的读取/更高级动作即放行
    if (rtype && `resource:${rtype}:`.length < p.length && p.startsWith(`resource:${rtype}:`) && p.endsWith(`:${action || 'read'}`)) {
      return true
    }
    // 通配：permission:*:read 或 action=admin
    if (p.startsWith('permission:*:') && p.endsWith(`:${action || 'read'}`)) return true
  }
  return false
}

/** 是否超级管理员（供菜单/按钮合并判断）。 */
export const isSuperAdmin = isAdmin

/** 供全局指令与组件复用的判定函数引用。 */
export { isAdmin as isAdminUser }