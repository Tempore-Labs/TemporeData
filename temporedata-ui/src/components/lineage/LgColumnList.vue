<template>
  <div class="lg-column-list">
    <el-input
      v-model="keyword"
      size="small"
      class="lg-col-search"
      placeholder="搜索字段"
      clearable
      :prefix-icon="Search"
      aria-label="Search columns"
      @update:model-value="emit('update:searchText', $event)"
    />
    <LgStatusTab :tabs="tabs" v-model="activeTab" class="lg-col-tabs" />

    <div class="lg-col-rows">
      <div v-if="!filtered.length" class="lg-col-empty">无匹配字段</div>
      <div
        v-for="c in filtered"
        :key="c.name"
        class="lg-col-row"
        :class="{ hl: highlight === c.name }"
        role="button"
        tabindex="0"
        :data-sort-column="c.name"
        @click="emit('select-column', c)"
        @keyup.enter="emit('select-column', c)"
      >
        <span class="lg-col-name">{{ c.name }}</span>
        <span v-if="c.type" class="lg-col-type">{{ c.type }}</span>
        <el-tag v-if="c.nullable" size="small" type="info" class="lg-col-null">可空</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Column list (C-12): the detail §2 field table. Has its own 5 internal tabs
 * (全部 / 源 / 目标 / 可空 / 主键) plus a leading search input. Clicking a row
 * emits `select-column` so the parent can toggle store column-highlighting.
 */
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import LgStatusTab from './LgStatusTab.vue'

const props = defineProps({
  columns: { type: Array, default: () => [] },
  highlight: { type: String, default: null },
  searchText: { type: String, default: '' }
})

const emit = defineEmits(['update:searchText', 'select-column', 'sort-change'])

const keyword = ref(props.searchText)
const activeTab = ref('all')

function countBy(pred) {
  return props.columns.filter(pred).length
}

const tabs = computed(() => [
  { key: 'all', label: '全部', count: props.columns.length },
  { key: 'source', label: '源', count: countBy((c) => !!c.source) },
  { key: 'target', label: '目标', count: countBy((c) => !!c.target) },
  { key: 'nullable', label: '可空', count: countBy((c) => !!c.nullable) },
  { key: 'pk', label: '主键', count: countBy((c) => !!c.pk) }
])

function matchesTab(c) {
  if (activeTab.value === 'all') return true
  if (activeTab.value === 'source') return !!c.source
  if (activeTab.value === 'target') return !!c.target
  if (activeTab.value === 'nullable') return !!c.nullable
  if (activeTab.value === 'pk') return !!c.pk
  return true
}

const filtered = computed(() => {
  const kw = (props.searchText || '').trim().toLowerCase()
  return props.columns.filter((c) => matchesTab(c) && (!kw || String(c.name).toLowerCase().includes(kw)))
})
</script>

<style scoped>
.lg-column-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.lg-col-search {
  width: 100%;
}
.lg-col-tabs {
  border-bottom: none;
  padding-bottom: 0;
}
.lg-col-rows {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 240px;
  overflow-y: auto;
}
.lg-col-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}
.lg-col-row:hover {
  background: #f1f5f9;
}
.lg-col-row.hl {
  background: #eef2ff;
  outline: 1px solid #8b5cf6;
}
.lg-col-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--lg-text, #0F172A);
  font-family: Menlo, Consolas, monospace;
  font-size: 11px;
}
.lg-col-type {
  color: var(--lg-text-muted, #64748B);
  font-size: 11px;
  flex-shrink: 0;
}
.lg-col-null {
  flex-shrink: 0;
  transform: scale(0.88);
}
.lg-col-empty {
  color: #94a3b8;
  font-size: 12px;
  padding: 10px 4px;
  text-align: center;
}
</style>