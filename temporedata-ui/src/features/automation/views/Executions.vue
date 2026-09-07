<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">自动化执行</h2>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-empty v-else-if="state === 'empty'" description="暂无执行记录" :image-size="80" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限查看自动化执行" />

    <DataTable v-else :rows="rows" :columns="columns" :state="state">
      <template #status="{ row }"><StatusBadge :value="EXECUTION_TONE[row.status] || 'unknown'" /></template>
    </DataTable>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { EXECUTION_TONE } from '../types'
import { listExecutions } from '../api'

const rows = ref([])
const { state, error, startLoading, finish, fail, deny } = usePageState()

const columns = [
  { prop: 'id', label: 'ID', width: 120 },
  { prop: 'runbookName', label: 'Runbook', width: 160 },
  { slot: 'status', label: '状态', width: 120 },
  { prop: 'triggeredBy', label: '触发人', width: 120 },
  { prop: 'startTime', label: '开始时间', width: 180 },
  { prop: 'endTime', label: '结束时间', width: 180 }
]

async function load() {
  startLoading()
  try { rows.value = (await listExecutions()) || []; finish(rows.value, rows.value.length === 0) } catch (e) { fail(e) }
}

onMounted(async () => { if (!hasPerm('automation:read')) { deny(); return } load() })
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-min-h { min-height: 120px; }
</style>