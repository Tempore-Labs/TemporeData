<template>
  <section class="v2-page">
    <h2 class="v2-page-title">作业 (v2.0 shell)</h2>
    <DataTable
      :rows="rows"
      :loading="state === 'loading'"
      :state="state"
      :columns="columns"
      @row-click="onRow"
    >
      <template #status="{ row }"><StatusBadge :value="JOB_TONE[row.status] ? row.status : 'unknown'" /></template>
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
import { JOB_TONE, routes } from '../routes'
import { listJobs } from '../api'

const router = useRouter()
const columns = [
  { prop: 'id', label: 'ID', width: 120 },
  { prop: 'name', label: '名称', width: 200 },
  { prop: 'type', label: '类型', width: 120 },
  { slot: 'status', label: '状态', width: 120, slotName: 'status' },
  { prop: 'updatedAt', label: '更新时间' }
]
const { state, startLoading, finish, fail, deny } = usePageState()
let rows = []

function onRow(row) { router.push(`/v2/jobs/${row.id}`).catch(() => {}) }

onMounted(async () => {
  if (!hasPerm('job:read')) { deny(); return }
  startLoading()
  try { rows = await listJobs({ page: 1, size: 20 }); finish(rows) } catch (e) { fail(e) }
})
</script>

<style scoped>
.v2-page-title { font-size: 18px; margin: 0 0 12px; }
</style>