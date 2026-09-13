<template>
  <div class="card" :class="{ hoverable }" :style="{ width }">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{ hoverable?: boolean; width?: string }>()
</script>

<style scoped>
.card {
  background: var(--td-surface);
  border: 1px solid var(--td-border);
  border-radius: var(--td-radius-lg);
  box-shadow: var(--td-card-shadow);
  transition: box-shadow 0.2s;
}
.card.hoverable:hover {
  box-shadow: var(--td-card-shadow-hover);
}
</style>
