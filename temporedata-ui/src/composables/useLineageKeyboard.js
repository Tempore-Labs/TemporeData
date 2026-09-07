/**
 * Global keyboard shortcuts for the lineage page (B4 / 06 §6.2).
 *
 * Installed in LineageV2's onMounted and uninstalled in onBeforeUnmount. Shortcuts
 * only take effect while the current route is the lineage page (route.name === 'Lineage')
 * so they never leak into other pages.
 *
 * Mapping:
 *   F2            -> focus the root asset search input
 *   Cmd/Ctrl + G  -> focus the canvas (activates its native keyboard pan/zoom)
 *   Cmd/Ctrl + D  -> open the "new edge" dialog (permission-gated upstream)
 *   Cmd/Ctrl + E  -> trigger the PNG export path (requires loaded graph)
 *   Cmd/Ctrl + Z  -> clear all layout overrides ("restore auto layout")
 *   Cmd/Ctrl + 1/2/3 -> switch graph level (1=table, 2=column, 3=table + DQ layer)
 */
import { useLineageStore } from '@/stores/lineage'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

/**
 * Install the keydown listener.
 *
 * @param {object} opts - Callbacks / element refs:
 *   - rootSearchEl : ref to the root asset-search control (el-select)
 *   - canvasEl     : ref to the focusable canvas wrapper div
 *   - onOpenNewEdge: () => void  — opens the new-edge write dialog
 *   - onExportPng  : () => void  — triggers the export command handler
 *   - notify       : (msg, type) => void — toast / announcement hook
 * @returns {() => void} Uninstall function (removes the listener).
 */
export function useLineageKeyboard({
  rootSearchEl,
  canvasEl,
  onOpenNewEdge,
  onExportPng,
  notify = ElMessage
}) {
  const store = useLineageStore()
  const route = useRoute()

  function setLevel(key) {
    if (key === '1') {
      if (store.graphLevel !== 'table') store.graphLevel = 'table'
    } else if (key === '2') {
      if (store.graphLevel !== 'column') store.graphLevel = 'column'
    } else if (key === '3') {
      if (store.graphLevel !== 'table') store.graphLevel = 'table'
      if (!store.layers.dq) store.toggleLayer('dq')
    }
  }

  function handler(e) {
    if (route.name !== 'Lineage') return

    // F2: focus root search (always, no modifier).
    if (e.key === 'F2') {
      e.preventDefault()
      try { rootSearchEl.value?.focus?.() } catch (_e) { /* ignore */ }
      return
    }

    // Modifier-only combos: Cmd/Ctrl + letter or number.
    if (!(e.metaKey || e.ctrlKey)) return

    const k = e.key.toLowerCase()
    switch (k) {
      case 'g':
        e.preventDefault()
        try { canvasEl.value?.focus?.() } catch (_e) { /* ignore */ }
        return
      case 'd':
        e.preventDefault()
        onOpenNewEdge()
        return
      case 'e':
        e.preventDefault()
        if (!store.graphData) {
          notify.warning?.('先加载图后才能导出')
          return
        }
        onExportPng()
        return
      case 'z':
        e.preventDefault()
        store.clearOverrides()
        notify.success?.('已恢复自动布局')
        return
      case '1':
      case '2':
      case '3':
        e.preventDefault()
        setLevel(k)
        return
      default:
        return
    }
  }

  window.addEventListener('keydown', handler)
  return () => window.removeEventListener('keydown', handler)
}