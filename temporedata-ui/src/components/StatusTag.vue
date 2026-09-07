<template>
  <span class="status-tag" :class="[`status-${type}`]">
    <span class="status-dot"></span>
    {{ label }}
  </span>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'info'
  },
  label: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 500;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.status-success { background: #ecfdf5; color: #065f46; }
.status-success .status-dot { background: #10b981; }
.status-warning { background: #fffbeb; color: #92400e; }
.status-warning .status-dot { background: #f59e0b; }
.status-danger { background: #fef2f2; color: #991b1b; }
.status-danger .status-dot { background: #ef4444; }
.status-info { background: #eff6ff; color: #1e40af; }
.status-info .status-dot { background: #3b82f6; }
.status-default { background: #f3f4f6; color: #374151; }
.status-default .status-dot { background: #9ca3af; }
</style>