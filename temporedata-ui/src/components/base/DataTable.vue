<template>
  <div class="data-table">
    <el-table
      v-bind="$attrs"
      :data="data"
      size="small"
      stripe=""
      table-layout="fixed"
      header-cell-class-name="td-table-header"
    >
      <slot />
    </el-table>
  </div>
</template>

<script setup lang="ts">
// Lightweight wrapper around el-table applying the Figma visual style:
// small size, light slate header (#f8fafc), row hover, fixed layout.
defineProps<{ data: unknown[] }>()
</script>

<style scoped>
.data-table {
  width: 100%;
}
.data-table :deep(.td-table-header) {
  background-color: #f8fafc !important;
  color: var(--td-text-2) !important;
  font-weight: 600;
}
</style>
