<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">事件</h2>
    </div>

    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-empty v-else-if="state === 'empty'" description="暂无事件" :image-size="80" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限查看事件" />

    <DataTable v-else :rows="rows" :columns="columns" :state="state" :loading="state === 'loading'" @row-click="onRow">
      <template #level="{ row }"><StatusBadge :value="INCIDENT_LEVEL_TONE[row.level] || 'unknown'" /></template>
      <template #status="{ row }"><StatusBadge :value="INCIDENT_TONE[row.status] || row.status || 'unknown'" /></template>
    </DataTable>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { INCIDENT_TONE, INCIDENT_LEVEL_TONE } from '../types'
import { listIncidents } from '../api'

const router = useRouter()
const columns = [
  { prop: 'id', label: 'ID', width: 120 },
  { slot: 'level', label: '级别', width: 100 },
  { prop: 'eventType', label: '事件类型', width: 140 },
  { prop: 'message', label: '事件内容', minWidth: 240 },
  { slot: 'status', label: '状态', width: 120 },
  { prop: 'createTime', label: '创建时间', width: 180 }
]
const { state, error, startLoading, finish, fail, deny } = usePageState()
let rows = []

function onRow(row) { router.push(`/v2/incidents/${row.id}`).catch(() => {}) }

async function load() {
  startLoading()
  try { rows = await listIncidents() || []; finish(rows, rows.length === 0) } catch (e) { fail(e) }
}

// Write actions live on the detail page (incident:operate, HIGH_RISK resolve).
onMounted(async () => { if (!hasPerm('incident:read')) { deny(); return } load() })
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-min-h { min-height: 120px; }
</style>