<template>
  <div class="search-table">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">{{ title }}</h2>
        <slot name="header-extra" />
      </div>
      <div class="page-header-right">
        <slot name="header-actions" />
        <el-button v-if="showAdd" type="primary" @click="$emit('add')">
          <el-icon><Plus /></el-icon>
          {{ addText }}
        </el-button>
      </div>
    </div>

    <!-- Toolbar -->
    <div v-if="$slots.toolbar" class="toolbar">
      <slot name="toolbar" />
    </div>

    <!-- Table Card -->
    <el-card shadow="never">
      <slot :loading="loading" />
      <!-- Pagination -->
      <div v-if="showPagination" class="pagination-wrapper">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="pageSizes"
          :total="total"
          :layout="paginationLayout"
          background
          @update:current-page="$emit('update:currentPage', $event)"
          @update:page-size="$emit('update:pageSize', $event)"
        />
      </div>
    </el-card>

    <!-- Dialog -->
    <slot name="dialog" />
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'

defineProps({
  title: { type: String, required: true },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  currentPage: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  pageSizes: { type: Array, default: () => [10, 20, 50] },
  paginationLayout: { type: String, default: 'total, sizes, prev, pager, next, jumper' },
  showAdd: { type: Boolean, default: true },
  showPagination: { type: Boolean, default: true },
  addText: { type: String, default: '新增' }
})

defineEmits(['add', 'update:currentPage', 'update:pageSize'])
</script>

<style scoped>
.search-table {
  padding: 0;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>