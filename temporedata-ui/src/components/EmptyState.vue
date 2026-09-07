<template>
  <div class="empty-state">
    <el-icon :size="64" color="#d1d5db"><component :is="icon" /></el-icon>
    <p class="empty-text">{{ text }}</p>
    <el-button v-if="actionLabel" type="primary" @click="$emit('action')">{{ actionLabel }}</el-button>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: 'FolderOpened' },
  text: { type: String, default: '暂无数据' },
  actionLabel: { type: String, default: '' }
})
defineEmits(['action'])
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 20px;
  gap: 16px;
}
.empty-text {
  font-size: 14px;
  color: #9ca3af;
  margin: 0;
}
</style>