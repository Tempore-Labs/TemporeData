<template>
  <div class="lg-status-tab" role="tablist" :aria-label="ariaLabel">
    <button
      v-for="t in tabs"
      :key="t.key"
      type="button"
      role="tab"
      class="lg-stab"
      :class="{ active: t.key === modelValue }"
      :aria-selected="t.key === modelValue"
      @click="onTab(t)"
    >
      <span class="lg-stab-label">{{ t.label }}</span>
      <span v-if="t.count != null" class="lg-stab-count">{{ t.count }}</span>
    </button>
  </div>
</template>

<script setup>
/**
 * Generic tab switcher (C-13) reused by the detail-panel §2 column tabs and the
 * §3 neighbor direction tabs. Items: { key, label, count? }.
 */
const props = defineProps({
  tabs: { type: Array, default: () => [] },
  modelValue: { type: String, default: '' },
  ariaLabel: { type: String, default: 'Tabs' }
})

const emit = defineEmits(['update:modelValue', 'change'])

function onTab(t) {
  if (t.key === props.modelValue) return
  const prev = props.modelValue
  emit('update:modelValue', t.key)
  emit('change', { key: t.key, prev })
}
</script>

<style scoped>
.lg-status-tab {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid #eef2f6;
  padding-bottom: 6px;
  flex-wrap: wrap;
}
.lg-stab {
  border: none;
  background: transparent;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  color: var(--lg-text-muted, #64748B);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  line-height: 1.4;
}
.lg-stab:hover {
  background: #F1F5F9;
  color: var(--lg-text, #0F172A);
}
.lg-stab.active {
  background: #EEF2FF;
  color: #1D4ED8;
  font-weight: 600;
}
.lg-stab-count {
  font-size: 10px;
  background: #E2E8F0;
  color: #475569;
  border-radius: 8px;
  padding: 0 5px;
  line-height: 1.5;
}
.lg-stab.active .lg-stab-count {
  background: #C7D2FE;
  color: #3730A3;
}
</style>