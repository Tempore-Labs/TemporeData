<template>
  <span class="badge" :style="{ background: color + '2e', color }">
    <span class="badge-dot" :style="{ background: color }" />
    {{ text }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const STATUS_COLORS: Record<string, string> = {
  success: '#10b981',
  passed: '#10b981',
  pass: '#10b981',
  running: '#2563eb',
  active: '#2563eb',
  ACTIVE: '#2563eb',
  failed: '#dc2626',
  fail: '#dc2626',
  error: '#dc2626',
  warning: '#f59e0b',
  warn: '#f59e0b',
  pending: '#f59e0b',
  open: '#f59e0b',
  OPEN: '#f59e0b',
  closed: '#94a3b8',
  RESOLVED: '#10b981',
  resolved: '#10b981',
}

const STATUS_LABELS: Record<string, string> = {
  success: '成功',
  passed: '通过',
  pass: '通过',
  running: '运行中',
  active: '启用',
  ACTIVE: '启用',
  failed: '失败',
  fail: '失败',
  error: '失败',
  warning: '警告',
  warn: '警告',
  pending: '待处理',
  open: '待处理',
  OPEN: '待处理',
  closed: '已关闭',
  RESOLVED: '已解决',
  resolved: '已解决',
}

const props = defineProps<{ status: string; text?: string; color?: string }>()

const color = computed(() => props.color || STATUS_COLORS[props.status] || '#64748b')
const text = computed(() => props.text || STATUS_LABELS[props.status] || props.status)
</script>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  border-radius: 99px;
  font-size: 10px;
  font-weight: 500;
  white-space: nowrap;
}
.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
</style>
