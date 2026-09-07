<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">作业详情</h2>
      <el-button size="small" @click="$router.back()">返回</el-button>
    </div>

    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限" />

    <template v-else-if="job">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="ID">{{ job.id }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ job.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ job.type }}</el-descriptions-item>
        <el-descriptions-item label="状态"><StatusBadge :value="JOB_TONE[job.status] ? job.status : 'unknown'" /></el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ job.updatedAt }}</el-descriptions-item>
      </el-descriptions>

      <h3 class="v2-subtitle">执行记录</h3>
      <DataTable :rows="executions" :columns="execColumns" :state="execState" @row-click="onExecution">
        <template #status="{ row }"><StatusBadge :value="EXECUTION_TONE[(row.status || '').toLowerCase()] ? row.status.toLowerCase() : 'unknown'" /></template>
      </DataTable>
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { JOB_TONE, EXECUTION_TONE } from '../routes'
import { getJob, getJobExecutions } from '../api'

const route = useRoute()
const router = useRouter()
const job = ref(null)
const executions = ref([])
const execColumns = [
  { prop: 'id', label: 'ID', width: 120 },
  { prop: 'executorType', label: '执行器', width: 140 },
  { slot: 'status', label: '状态', width: 120 },
  { prop: 'traceId', label: 'Trace ID', width: 200 }
]
const { state, error, startLoading, finish, fail, deny } = usePageState()
const execState = usePageState()

function onExecution(row) { router.push(`/v2/jobs/${route.params.id}/executions/${row.id}`).catch(() => {}) }

onMounted(async () => {
  if (!hasPerm('job:read')) { deny(); return }
  const id = route.params.id
  startLoading()
  try {
    job.value = await getJob(id)
    try {
      executions.value = await getJobExecutions(id)
      execState.finish(executions.value, executions.value.length === 0)
    } catch (e) { execState.fail(e) }
    finish(job.value)
  } catch (e) { fail(e) }
})
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-subtitle { margin: 20px 0 8px; font-size: 15px; }
.v2-min-h { min-height: 120px; }
</style>