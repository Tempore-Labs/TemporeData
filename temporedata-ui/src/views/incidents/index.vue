<template>
  <div class="page">
    <PageHeader title="事件闭环" subtitle="质量事件登记、跟踪与 RCA 联动">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 登记事件</el-button>
      </template>
    </PageHeader>

    <div class="stat-row">
      <Card class="open-card">
        <span class="open-label">未关闭事件</span>
        <span class="open-value">{{ openCount }}</span>
      </Card>
    </div>

    <div class="filter-bar">
      <el-select v-model="statusFilter" size="small" style="width: 160px" clearable placeholder="全部状态" @change="loadByFilter">
        <el-option v-for="s in ['OPEN', 'INVESTIGATING', 'RESOLVED', 'CLOSED']" :key="s" :label="s" :value="s" />
      </el-select>
      <el-button size="small" @click="loadAll">刷新</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="sourceType" label="来源" width="100">
        <template #default="{ row }"><el-tag size="small">{{ row.sourceType || '—' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="datasetId" label="数据集ID" width="100" />
      <el-table-column prop="severity" label="级别" width="100">
        <template #default="{ row }">
          <StatusBadge :status="severityStatus(row.severity)" :text="row.severity || '—'" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-select v-model="row.status" size="small" style="width: 120px" @change="(v: string) => changeStatus(row, v)">
            <el-option v-for="s in ['OPEN', 'INVESTIGATING', 'RESOLVED', 'CLOSED']" :key="s" :label="s" :value="s" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="150">
        <template #default="{ row }"><span class="mono muted">{{ fmtTime(row.createdAt) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="viewRca(row)">RCA</el-button>
          <el-button size="small" text type="primary" @click="openRca(row)">分析</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <!-- Create dialog -->
    <el-dialog v-model="dialogVisible" title="登记事件" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="form.sourceType" style="width: 100%">
            <el-option v-for="t in ['QUALITY', 'SYNC', 'API', 'INFRA', 'MANUAL']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据集ID">
          <el-input-number v-model="form.datasetId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="form.severity" style="width: 100%">
            <el-option v-for="s in ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="RCA 分析">
          <el-input v-model="form.rca" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">登记</el-button>
      </template>
    </el-dialog>

    <!-- RCA dialog -->
    <el-dialog v-model="rcaVisible" :title="`RCA 分析 · 事件 #${rcaTarget?.id ?? ''}`" width="640px">
      <el-skeleton v-if="rcaLoading" :rows="8" animated />
      <template v-else-if="plan">
        <el-alert type="info" :closable="false" style="margin-bottom: 12px">
          <template #title>
            <b>{{ findingText }}</b>
          </template>
          <p class="rca-hypo">{{ plan.finding.hypothesis }}</p>
        </el-alert>
        <div class="action-list">
          <div v-for="(a, i) in plan.actions" :key="i" class="action-item">
            <div class="action-head">
              <span class="action-title">{{ a.title }}</span>
              <StatusBadge :status="a.allowed ? 'pass' : a.requiresApproval ? 'warning' : 'failed'" :text="a.allowed ? '可执行' : a.requiresApproval ? '需审批' : '禁止'" />
            </div>
            <div class="action-cmd mono">{{ a.suggestedCommand }}</div>
            <div class="action-reason">{{ a.reason }}</div>
          </div>
        </div>
      </template>
      <el-empty v-else description="无分析结果" :image-size="60" />
      <template #footer>
        <el-button @click="rcaVisible = false">关闭</el-button>
        <el-button
          v-if="plan"
          type="primary"
          :loading="applying"
          @click="doApply(false)"
        >应用自动项</el-button>
        <el-button
          v-if="plan && plan.actions.some((a) => a.requiresApproval)"
          type="warning"
          :loading="applying"
          @click="doApply(true)"
        >批准并全部应用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import Card from '@/components/base/Card.vue'
import { incidentApi } from '@/api/incident'
import { rcaApi } from '@/api/rca'
import type { IncidentEntity } from '@/types/incident'
import type { ActionPlan } from '@/types/rca'

const queryClient = useQueryClient()
const rows = ref<IncidentEntity[]>([])
const loading = ref(false)
const openCount = ref(0)
const statusFilter = ref('')

const dialogVisible = ref(false)
const saving = ref(false)
const form = ref<{ title: string; sourceType?: string; datasetId?: number; severity?: string; rca?: string }>({ title: '' })

const rcaVisible = ref(false)
const rcaLoading = ref(false)
const rcaTarget = ref<IncidentEntity | null>(null)
const plan = ref<ActionPlan | null>(null)
const applying = ref(false)

async function loadAll() {
  loading.value = true
  try {
    rows.value = statusFilter.value ? await incidentApi.byStatus(statusFilter.value) : await incidentApi.byStatus('OPEN')
    // byStatus('OPEN') only returns open; to show all, fetch open + closed separately when no filter.
    if (!statusFilter.value) {
      const closed = await incidentApi.byStatus('CLOSED').catch(() => [])
      const resolved = await incidentApi.byStatus('RESOLVED').catch(() => [])
      const inv = await incidentApi.byStatus('INVESTIGATING').catch(() => [])
      rows.value = [...rows.value, ...inv, ...resolved, ...closed]
    }
  } finally {
    loading.value = false
  }
}

async function loadCount() {
  try {
    openCount.value = await incidentApi.openCount()
  } catch {
    openCount.value = 0
  }
}

loadAll()
loadCount()

function loadByFilter() {
  loadAll()
}

function openCreate() {
  form.value = { title: '', sourceType: 'QUALITY', datasetId: 1, severity: 'MEDIUM' }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.title) {
    ElMessage.warning('标题必填')
    return
  }
  saving.value = true
  try {
    await incidentApi.open(form.value.title, form.value.sourceType, form.value.datasetId, form.value.severity, form.value.rca)
    ElMessage.success('事件已登记')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['incident'] })
    await loadAll()
    await loadCount()
  } finally {
    saving.value = false
  }
}

async function changeStatus(row: IncidentEntity, status: string) {
  await incidentApi.updateStatus(row.id, status)
  ElMessage.success(`状态已更新为 ${status}`)
  await loadCount()
}

function openRca(row: IncidentEntity) {
  rcaTarget.value = row
  plan.value = null
  rcaVisible.value = true
  rcaLoading.value = true
  rcaApi
    .analyze(row.id)
    .then((p) => (plan.value = p))
    .finally(() => (rcaLoading.value = false))
}

async function viewRca(row: IncidentEntity) {
  if (!row.rcaAnalysis) {
    ElMessage.info('该事件暂无 RCA 分析记录，可点击"分析"生成')
    return
  }
  await ElMessageBox.alert(row.rcaAnalysis, `RCA · 事件 #${row.id}`, { confirmButtonText: '关闭' })
}

async function doApply(approve: boolean) {
  if (!rcaTarget.value) return
  applying.value = true
  try {
    const res = await rcaApi.apply(rcaTarget.value.id, approve)
    ElMessage.success(`已应用 ${res.applied} 项，待审批 ${res.needsApproval} 项，状态: ${res.status}`)
    rcaVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['incident'] })
    await loadAll()
    await loadCount()
  } finally {
    applying.value = false
  }
}

const findingText = () => {
  const p = plan.value
  if (!p) return ''
  return `根因 ${p.finding.causeType} · 置信度 ${(p.finding.confidence * 100).toFixed(0)}%`
}

function severityStatus(s?: string) {
  if (s === 'CRITICAL' || s === 'HIGH') return 'failed'
  if (s === 'MEDIUM') return 'warning'
  return 'success'
}
function fmtTime(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 19) : '—'
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.stat-row {
  margin-bottom: 16px;
}
.open-card {
  display: inline-flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
}
.open-label {
  font-size: 13px;
  color: var(--td-text-3);
}
.open-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--td-danger);
  font-family: var(--td-font-mono);
}
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.mono {
  font-family: var(--td-font-mono);
}
.muted {
  color: var(--td-text-4);
}
.rca-hypo {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--td-text-2);
}
.action-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
  max-height: 320px;
  overflow: auto;
}
.action-item {
  padding: 12px 14px;
  border: 1px solid var(--td-border);
  border-radius: 10px;
  background: var(--td-bg);
}
.action-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.action-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--td-text-1);
}
.action-cmd {
  font-size: 11px;
  color: var(--td-primary);
  margin-top: 6px;
}
.action-reason {
  font-size: 11px;
  color: var(--td-text-4);
  margin-top: 4px;
}
</style>
