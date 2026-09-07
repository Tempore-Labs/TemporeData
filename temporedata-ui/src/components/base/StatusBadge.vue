<template>
  <span class="td-badge" :class="`is-${tone}`"><i class="dot" />{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  value: { type: String, default: '' },
  toneMap: { type: Object, default: () => ({ healthy: 'success', running: 'primary', warning: 'warning', critical: 'danger', stopped: 'info', unknown: 'info', maintenance: 'warning' }) }
})
const tone = computed(() => props.toneMap[props.value] || 'info')
const label = computed(() => (props.value === '' ? 'unknown' : props.value))
</script>

<style scoped>
.td-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; padding: 2px 8px; border-radius: 999px; background: #f1f5f9; color: #475569; }
.td-badge .dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.is-success { color: #059669; background: #ecfdf5; }
.is-danger { color: #dc2626; background: #fef2f2; }
.is-warning { color: #d97706; background: #fffbeb; }
.is-primary { color: #2563eb; background: #eff6ff; }
</style>