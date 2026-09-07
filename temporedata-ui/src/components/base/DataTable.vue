<template>
  <div class="td-datatable">
    <div v-if="state === 'loading'" v-loading="true" class="td-datatable-box" />
    <el-empty v-else-if="state === 'empty'" description="暂无数据" :image-size="60" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="errorText" />
    <el-table v-else v-loading="loading" :data="rows" border stripe @row-click="$emit('row-click', $event)">
      <el-table-column v-for="c in columns" :key="c.prop" :prop="c.prop" :label="c.label" :min-width="c.width" show-overflow-tooltip>
        <template v-if="c.slot" #default="{ row }"><slot :name="c.slot" :row="row" /></template>
      </el-table-column>
      <template #empty>暂无数据</template>
    </el-table>
  </div>
</template>

<script setup>
defineProps({ rows: { type: Array, default: () => [] }, columns: { type: Array, default: () => [] }, loading: { type: Boolean, default: false }, state: { type: String, default: 'success' }, errorText: { type: String, default: '加载失败' } })
defineEmits(['row-click'])
</script>

<style scoped>
.td-datatable { width: 100%; }
.td-datatable-box { min-height: 120px; }
</style>