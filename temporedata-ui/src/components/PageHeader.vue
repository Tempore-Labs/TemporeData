<template>
  <div class="page-header">
    <div class="page-header-left">
      <el-icon v-if="icon" class="page-icon" :size="22"><component :is="icon" /></el-icon>
      <h2 class="page-title">{{ title }}</h2>
      <span class="page-desc" v-if="desc">{{ desc }}</span>
    </div>
    <div class="page-header-right">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  desc: { type: String, default: '' },
  icon: { type: Object, default: null }
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.page-icon {
  color: #409eff;
  flex-shrink: 0;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}
.page-desc {
  font-size: 13px;
  color: #9ca3af;
}
.page-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>