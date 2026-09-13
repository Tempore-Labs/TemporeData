<template>
  <div class="inst">
    <div class="inst-bar">
      <el-button size="small" type="primary" plain @click="openBackfill">补数</el-button>
      <el-button size="small" @click="load">刷新</el-button>
    </div>
    <el-table :data="rows" size="small" :loading="loading" empty-text="暂无实例">
      <el-table-column label="实例 ID" width="170">
        <template #default="{ row }"><span class="mono">{{ shortId(row.id) }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
      </el-table-column>
      <el-table-column prop="triggerType" label="触发" width="90" />
      <el-table-column prop="startTime" label="开始时间" min-width="150">
        <template #default="{ row }"><span class="mono muted">{{ row.startTime || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <template v-if="row.status === 'RUNNING' || row.status === 'PAUSED'">
            <el-button size="small" text :disabled="row.status === 'PAUSED'" @click="act('pause', row)">暂停</el-button>
            <el-button size="small" text :disabled="row.status !== 'PAUSED'" @click="act('resume', row)">恢复</el-button>
          </template>
          <el-button size="small" text type="danger" @click="act('stop', row)">停止</el-button>
          <el-button size="small" text type="warning" @click="act('rerun', row)">重跑</el-button>
          <el-button size="small" text type="warning" @click="act('recover', row)">恢复失败</el-button>
          <el-button size="small" type="primary" text @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- backfill dialog -->
    <el-dialog v-model="bfOpen" title="补数（历史批量重算）" width="460px">
      <el-form label-width="90px" size="small">
        <el-form-item label="日期区间" required>
          <el-date-picker v-model="bfRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" style="width: 100%" />
        </el-form-item>
        <el-form-item label="并发方式">
          <el-radio-group v-model="bfConcurrency">
            <el-radio value="SERIAL">串行</el-radio>
            <el-radio value="PARALLEL">并行</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="按">
          <el-radio-group v-model="bfInterval">
            <el-radio value="DAILY">按天</el-radio>
            <el-radio value="HOURLY">按小时</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bfOpen = false">取消</el-button>
        <el-button type="primary" :loading="bfSaving" @click="submitBackfill">开始补数</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawerOpen" title="实例详情" size="560px">
      <template v-if="detail">
        <div class="inst-detail-head">
          <StatusBadge :status="detail.status" :text="detail.status" />
          <span class="mono muted">{{ detail.startTime }} ~ {{ detail.finishTime }}</span>
        </div>

        <el-divider content-position="left">节点</el-divider>
        <el-table :data="detail.nodes || []" size="small">
          <el-table-column prop="nodeName" label="节点" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
          </el-table-column>
          <el-table-column label="耗时" width="80">
            <template #default="{ row }"><span class="mono">{{ row.durationMs ?? '—' }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="170">
            <template #default="{ row }">
              <el-button size="small" text type="success" :disabled="row.status === 'SUCCESS'" @click="forceSuccess(row)">强制成功</el-button>
              <el-button size="small" text type="primary" @click="showLogs(row)">日志</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-divider content-position="left">节点日志</el-divider>
        <div class="inst-logbar">
          <el-input v-model="logKeyword" size="small" placeholder="搜索日志关键词" clearable style="width: 220px" />
          <el-button size="small" @click="downloadLogs">下载日志</el-button>
        </div>
        <pre class="inst-log">{{ filteredLogs }}</pre>
      </template>
      <el-empty v-else description="加载中…" :image-size="50" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusBadge from '@/components/base/StatusBadge.vue'
import {
  workflowApi,
  type WorkflowInstanceRes,
  type WorkflowNodeInstanceRes,
  type InstanceLogItem,
} from '@/api/workflow'

const props = defineProps<{ workflow?: { id: string } | null }>()

const rows = ref<WorkflowInstanceRes[]>([])
const loading = ref(false)
const drawerOpen = ref(false)
const detail = ref<WorkflowInstanceRes | null>(null)
const logs = ref<InstanceLogItem[]>([])
const logKeyword = ref('')

// backfill
const bfOpen = ref(false)
const bfRange = ref<[string, string] | null>(null)
const bfConcurrency = ref('SERIAL')
const bfInterval = ref('DAILY')
const bfSaving = ref(false)

const filteredLogs = computed(() => {
  const kw = logKeyword.value.trim().toLowerCase()
  return logs.value
    .map((l) => `[${l.level}] ${l.createTime || ''} ${l.message}`)
    .filter((s) => !kw || s.toLowerCase().includes(kw))
    .join('\n')
})

async function load() {
  if (!props.workflow?.id) { rows.value = []; return }
  loading.value = true
  try {
    rows.value = (await workflowApi.instances(props.workflow.id)) || []
  } finally {
    loading.value = false
  }
}

watch(
  () => props.workflow?.id,
  (id) => { if (id) { load(); if (drawerOpen.value) drawerOpen.value = false } },
  { immediate: true },
)

function shortId(id: string): string {
  return id && id.length > 12 ? id.slice(0, 12) : id || '—'
}

function openBackfill() {
  if (!props.workflow?.id) { ElMessage.warning('请先保存工作流'); return }
  bfRange.value = null
  bfOpen.value = true
}

async function submitBackfill() {
  if (!props.workflow?.id) return
  if (!bfRange.value?.[0] || !bfRange.value?.[1]) { ElMessage.warning('请选择日期区间'); return }
  bfSaving.value = true
  try {
    const res = await workflowApi.backfill(props.workflow.id, {
      start: bfRange.value[0],
      end: bfRange.value[1],
      interval: bfInterval.value,
      concurrency: bfConcurrency.value,
    })
    ElMessage.success(`补数完成，生成 ${res.generated} 个实例`)
    bfOpen.value = false
    await load()
  } finally {
    bfSaving.value = false
  }
}

async function act(type: string, row: WorkflowInstanceRes) {
  const fns: Record<string, () => Promise<any>> = {
    pause: () => workflowApi.pause(row.id),
    resume: () => workflowApi.resume(row.id),
    stop: () => workflowApi.stop(row.id),
    rerun: () => workflowApi.rerun(row.id, 'ALL'),
    recover: () => workflowApi.recoverFailed(row.id),
  }
  await fns[type]()
  ElMessage.success({ pause: '已暂停', resume: '已恢复', stop: '已停止', rerun: '已重跑', recover: '已发起恢复失败' }[type])
  await load()
}

async function openDetail(row: WorkflowInstanceRes) {
  drawerOpen.value = true
  detail.value = await workflowApi.instanceDetail(row.id)
  logs.value = await workflowApi.logs(row.id)
}

function forceSuccess(node: WorkflowNodeInstanceRes) {
  if (!detail.value) return
  ElMessageBox.confirm(`将节点 ${node.nodeName} 强制标记为成功？`, '提示', { type: 'warning' })
    .then(async () => {
      await workflowApi.forceSuccess(detail.value!.id, node.id)
      ElMessage.success('已强制成功')
      detail.value = await workflowApi.instanceDetail(detail.value!.id)
    })
    .catch(() => {})
}

async function showLogs(_node: WorkflowNodeInstanceRes) {
  if (!detail.value) return
  logs.value = await workflowApi.logs(detail.value.id)
  logKeyword.value = ''
}

function downloadLogs() {
  const blob = new Blob([filteredLogs.value], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `instance-${shortId(detail.value?.id || '')}.log`
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.inst { padding: 16px; }
.inst-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.inst-detail-head { display: flex; align-items: center; gap: 10px; }
.inst-logbar { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.inst-log {
  max-height: 300px; overflow: auto; background: #1e1e1e; color: #d4d4d4;
  font-size: 12px; padding: 8px; border-radius: 6px; white-space: pre-wrap; word-break: break-all;
}
.mono { font-family: var(--td-font-mono); }
.muted { color: var(--el-text-color-secondary); }
</style>