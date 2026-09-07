/**
 * Unified page UI state machine for v2.0 feature views.
 * Per 03-frontend-code-architecture.md: loading / empty / error / success / permission-denied / offline.
 */
import { computed, readonly, ref } from 'vue'

export const PAGE_STATE = Object.freeze(['loading', 'empty', 'error', 'success', 'permission-denied', 'offline'])

/**
 * @param {{ loading?: boolean, empty?: boolean, denied?: boolean, offline?: boolean }} [initial]
 */
export function usePageState(initial = {}) {
  const loading = ref(initial.loading ?? false)
  const empty = ref(initial.empty ?? false)
  const denied = ref(initial.denied ?? false)
  const offline = ref(initial.offline ?? false)
  const error = ref(null)

  const state = computed(() => {
    if (offline.value) return 'offline'
    if (denied.value) return 'permission-denied'
    if (loading.value) return 'loading'
    if (error.value) return 'error'
    if (empty.value) return 'empty'
    return 'success'
  })

  function startLoading() { loading.value = true; error.value = null }
  function finish(value, isEmpty = false) { loading.value = false; empty.value = value != null && isEmpty; error.value = null; offline.value = false }
  function fail(err) { loading.value = false; error.value = err; empty.value = false }
  function offline_(v = true) { offline.value = v }
  function deny(v = true) { denied.value = v }

  return { state: readonly(state), loading, empty, denied, offline, error, startLoading, finish, fail, offline_, deny }
}