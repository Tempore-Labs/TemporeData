<template>
  <el-card class="td-metric" shadow="never">
    <div class="m-label">{{ label }}</div>
    <div class="m-value" :class="{ accent }">{{ value }}</div>
    <div v-if="hint" class="m-hint">{{ hint }}</div>
  </el-card>
</template>

<script setup>
defineProps({ label: { type: String, default: '' }, value: { type: [String, Number], default: '-' }, hint: { type: String, default: '' }, accent: { type: Boolean, default: false } })
</script>

<style scoped>
.td-metric { border: 1px solid var(--td-border, #e2e8f0); border-radius: 10px; }
.m-label { font-size: 12px; color: #64748b; }
.m-value { font-size: 24px; font-weight: 700; margin-top: 4px; }
.m-value.accent { color: #2563eb; }
.m-hint { font-size: 12px; color: #94a3b8; margin-top: 4px; }
</style>