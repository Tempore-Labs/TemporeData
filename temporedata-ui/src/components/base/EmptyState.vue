<template>
  <div class="empty-state">
    <div class="empty-icon">
      <el-icon :size="30"><component :is="icon" /></el-icon>
    </div>
    <div class="empty-title">{{ title }}</div>
    <div v-if="desc" class="empty-desc">{{ desc }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ icon?: string; title: string; desc?: string }>()
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px;
  text-align: center;
}
.empty-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--td-border-light);
  color: var(--td-text-3);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.empty-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--td-text-2);
}
.empty-desc {
  font-size: 12px;
  color: var(--td-text-4);
  margin-top: 4px;
}
</style>
