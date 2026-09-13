<template>
  <div class="stat-card" @click="$emit('click')">
    <div class="stat-icon" :style="{ background: colorSoft, color }">
      <el-icon :size="18"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-value">{{ value }}</div>
    <div class="stat-label">{{ label }}</div>
    <div v-if="sub" class="stat-sub" :style="{ color }">{{ sub }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon: string
  value: string | number
  label: string
  sub?: string
  color: string
  colorSoft: string
}>()

defineEmits<{ (e: 'click'): void }>()
</script>

<style scoped>
.stat-card {
  padding: 16px;
  background: var(--td-surface);
  border: 1px solid var(--td-border);
  border-radius: var(--td-radius-lg);
  box-shadow: var(--td-card-shadow);
  transition: box-shadow 0.2s;
  cursor: default;
}
.stat-card:hover {
  box-shadow: var(--td-card-shadow-hover);
}
.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
  color: var(--td-text-1);
  font-variant-numeric: tabular-nums;
}
.stat-label {
  font-size: 12px;
  color: var(--td-text-3);
  margin-top: 4px;
}
.stat-sub {
  font-size: 10px;
  margin-top: 4px;
}
</style>
