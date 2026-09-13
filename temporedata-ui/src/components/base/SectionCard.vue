<template>
  <div class="section-card">
    <div class="section-head">
      <div>
        <div class="section-title">{{ title }}</div>
        <div v-if="sub" class="section-sub">{{ sub }}</div>
      </div>
      <div v-if="$slots.extra" class="section-extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="section-body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; sub?: string }>()
</script>

<style scoped>
.section-card {
  background: var(--td-surface);
  border: 1px solid var(--td-border);
  border-radius: var(--td-radius-lg);
  box-shadow: var(--td-card-shadow);
  overflow: hidden;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 12px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--td-text-1);
}
.section-sub {
  font-size: 12px;
  color: var(--td-text-4);
  margin-top: 2px;
}
.section-body {
  padding: 0 20px 20px;
}
</style>
