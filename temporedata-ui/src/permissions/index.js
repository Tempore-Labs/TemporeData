// v2.0 permission helpers. Re-uses the existing global permission evaluator so the
// shell shares one source of truth with the legacy app. Per-module gates go here.
import { hasPerm as hasPermGlobal, isAdminUser } from '@/lib/permission'
import { useAuthStore } from '@/stores/auth'

export function hasPerm(perm, user) {
  return hasPermGlobal(perm, user || useAuthStore().user)
}

// High-risk actions: must go through Action Preview / Approval, never Command Palette.
export const HIGH_RISK = Object.freeze(['job:execute', 'automation:item:execute', 'incident:resolve', 'data:drop'])

export function canExecute(perm, user) {
  if (isAdminUser(user || useAuthStore().user)) return true
  return hasPerm(perm, user || useAuthStore().user) && !HIGH_RISK.includes(perm)
}