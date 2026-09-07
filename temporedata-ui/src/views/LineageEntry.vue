<template>
  <component :is="currentView" />
</template>

<script setup>
/**
 * Feature-flag dispatcher between the new 4-block LineageV2 (default) and the
 * legacy 3-block Lineage view. Route name/title and menu permission are unchanged.
 *
 * Selection precedence:
 *  1. Explicit `?layout=v2|legacy` query param.
 *  2. Persisted user preference in localStorage key `td_lineage_layout`.
 *  3. Default "v2".
 */
import { ref, computed, watch, onMounted, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'

// Async so each layout is code-split into its own chunk (avoids bundling the
// legacy + v2 + shared G6 graph into a single ~1.5MB entry chunk on first load).
const Lineage = defineAsyncComponent(() => import('@/views/Lineage.vue'))
const LineageV2 = defineAsyncComponent(() => import('@/views/LineageV2.vue'))

const route = useRoute()
const active = ref('v2')

const currentView = computed(() => (active.value === 'legacy' ? Lineage : LineageV2))

function resolveLayout(q) {
  if (q === 'legacy') return 'legacy'
  if (q === 'v2') return 'v2'
  // No explicit query -> fall back to stored preference (default v2).
  try {
    const stored = localStorage.getItem('td_lineage_layout')
    if (stored === 'legacy') return 'legacy'
  } catch (e) { /* ignore storage access failures */ }
  return 'v2'
}

onMounted(() => {
  active.value = resolveLayout(route.query.layout)
  persistChoice(active.value)
})

// React to in-place query changes without remounting (e.g. link with ?layout=legacy).
watch(
  () => route.query.layout,
  (q) => {
    if (q) {
      active.value = resolveLayout(q)
      persistChoice(active.value)
    }
  }
)

function persistChoice(layout) {
  try {
    localStorage.setItem('td_lineage_layout', layout)
  } catch (e) { /* ignore storage access failures */ }
}
</script>