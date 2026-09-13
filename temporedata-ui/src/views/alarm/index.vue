<template>
  <div class="page">
    <PageHeader title="告警配置" subtitle="告警规则、基线与告警记录">
      <template #actions>
        <el-button size="small" :loading="checking" @click="runCheck">运行基线检查</el-button>
        <el-button size="small" type="primary" @click="openCreate(tab)">+ 新建</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- 告警规则 -->
      <el-tab-pane label="告警规则" name="config">
        <el-skeleton v-if="configLoading" :rows="6" animated />
        <DataTable v-else :data="configRows">
          <el-table-column prop="name" label="规则名称" min-width="150" />
          <el-table-column prop="eventType" label="事件类型" width="130">
            <template #default="{ row }"><el-tag size="small">{{ row.eventType || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="channels" label="通道" width="110">
            <template #default="{ row }"><el-tag size="small" type="info">{{ row.channels || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
          <el-table-column prop="message" label="消息体" min-width="220" show-overflow-tooltip />
          <el-table-column label="启用" width="90">
            <template #default="{ row }">
              <StatusBadge :status="row.enabled === false ? 'closed' : 'success'" :text="row.enabled === false ? '停用' : '启用'" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="testConfig(row)">测试</el-button>
              <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="removeConfig(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>

      <!-- 基线告警 -->
      <el-tab-pane label="基线告警" name="baseline">
        <el-skeleton v-if="baselineLoading" :rows="6" animated />
        <DataTable v-else :data="baselineRows">
          <el-table-column prop="name" label="基线名称" min-width="150" />
          <el-table-column prop="workflowId" label="工作流" min-width="140">
            <template #default="{ row }"><span class="mono">{{ row.workflowId }}</span></template>
          </el-table-column>
          <el-table-column prop="expectTime" label="期望完成" width="120">
            <template #default="{ row }"><span class="mono">{{ row.expectTime }}</span></template>
          </el-table-column>
          <el-table-column prop="bizDateMode" label="业务日期" width="110">
            <template #default="{ row }">{{ row.bizDateMode || '—' }}</template>
          </el-table-column>
          <el-table-column prop="triggerType" label="触发类型" width="110">
            <template #default="{ row }"><el-tag size="small">{{ row.triggerType || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="graceSeconds" label="宽限(s)" width="100">
            <template #default="{ row }"><span class="mono">{{ row.graceSeconds ?? '—' }}</span></template>
          </el-table-column>
          <el-table-column label="启用" width="90">
            <template #default="{ row }">
              <el-switch :model-value="!!row.enabled" @change="(v: boolean) => toggleBaseline(row, v)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="testBaseline(row)">测试</el-button>
              <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="removeBaseline(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>

      <!-- 告警记录 -->
      <el-tab-pane label="告警记录" name="record">
        <div class="filter-bar">
          <el-select v-model="recordStatus" size="small" style="width: 160px" clearable placeholder="全部状态" @change="loadRecords">
            <el-option v-for="s in ['PENDING', 'SENT', 'ACK', 'CLOSED']" :key="s" :label="s" :value="s" />
          </el-select>
        </div>
        <el-skeleton v-if="recordLoading" :rows="6" animated />
        <DataTable v-else :data="recordRows">
          <el-table-column prop="alarmType" label="类型" width="110">
            <template #default="{ row }"><el-tag size="small" :type="typeTag(row.alarmType)">{{ row.alarmType }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="baselineId" label="基线" min-width="140">
            <template #default="{ row }"><span class="mono">{{ row.baselineId }}</span></template>
          </el-table-column>
          <el-table-column prop="bizDate" label="业务日期" width="110">
            <template #default="{ row }"><span class="mono">{{ row.bizDate || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
          <el-table-column label="状态" width="110">
            <template #default="{ row }"><StatusBadge :status="row.status" /></template>
          </el-table-column>
          <el-table-column prop="createTime" label="触发时间" min-width="150">
            <template #default="{ row }"><span class="mono muted">{{ row.createTime || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status !== 'ACK' && row.status !== 'CLOSED'" size="small" text type="primary" @click="ack(row)">确认</el-button>
              <el-button v-if="row.status !== 'CLOSED'" size="small" text type="danger" @click="close(row)">关闭</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>
    </el-tabs>

    <!-- Create/edit dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="540px">
      <el-form v-if="tab === 'config'" :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="form.eventType" style="width: 100%">
            <el-option v-for="t in ['TASK_FAILED', 'WORKFLOW_FAILED', 'QUALITY_FAILED', 'DATASOURCE_DOWN']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="通道">
          <el-select v-model="form.channels" style="width: 100%">
            <el-option v-for="c in ['EMAIL', 'WEBHOOK', 'SMS', 'ALL']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.channels?.includes('EMAIL')" label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item v-if="form.channels?.includes('WEBHOOK')" label="Webhook">
          <el-input v-model="form.webhookUrl" />
        </el-form-item>
        <el-form-item label="消息体">
          <el-input v-model="form.message" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>

      <el-form v-else :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="baseForm.name" />
        </el-form-item>
        <el-form-item label="工作流" required>
          <el-input v-model="baseForm.workflowId" placeholder="工作流 ID" />
        </el-form-item>
        <el-form-item label="期望完成" required>
          <el-time-picker v-model="expectTimeVal" value-format="HH:mm:ss" format="HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="业务日期模式">
          <el-select v-model="baseForm.bizDateMode" style="width: 100%">
            <el-option v-for="m in ['DAY', 'WEEK', 'MONTH']" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="触发类型">
          <el-select v-model="baseForm.triggerType" style="width: 100%">
            <el-option v-for="t in ['MISS', 'DELAY']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="宽限(秒)">
          <el-input-number v-model="baseForm.graceSeconds" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="通知通道">
          <el-input v-model="baseForm.notifyChannels" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="baseForm.operator" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="baseForm.remark" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="baseForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { alarmApi, type AlarmBaselineEntity, type AlarmEntity, type AlarmRecordEntity } from '@/api/alarm'

const queryClient = useQueryClient()
const tab = ref('config')
const checking = ref(false)

// config
const configRows = ref<AlarmEntity[]>([])
const configLoading = ref(false)
// baseline
const baselineRows = ref<AlarmBaselineEntity[]>([])
const baselineLoading = ref(false)
// record
const recordRows = ref<AlarmRecordEntity[]>([])
const recordLoading = ref(false)
const recordStatus = ref('')

const dialogVisible = ref(false)
const editing = ref<AlarmEntity | AlarmBaselineEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<AlarmEntity>>({})
const baseForm = ref<Partial<AlarmBaselineEntity>>({})
const expectTimeVal = ref('08:00:00')

const dialogTitle = computed(() => {
  const t = editing.value ? '编辑' : '新建'
  return tab.value === 'config' ? `${t}告警规则` : `${t}基线告警`
})

async function loadConfig() {
  configLoading.value = true
  try {
    configRows.value = await alarmApi.listConfig()
  } finally {
    configLoading.value = false
  }
}
async function loadBaselines() {
  baselineLoading.value = true
  try {
    baselineRows.value = await alarmApi.listBaselines()
  } finally {
    baselineLoading.value = false
  }
}
async function loadRecords() {
  recordLoading.value = true
  try {
    recordRows.value = await alarmApi.records(undefined, recordStatus.value || undefined)
  } finally {
    recordLoading.value = false
  }
}
loadConfig()

function onTabChange(name: string | number) {
  if (name === 'baseline') loadBaselines()
  else if (name === 'record') loadRecords()
}

function openCreate(type: string) {
  editing.value = null
  if (type === 'config') {
    form.value = { name: '', eventType: 'TASK_FAILED', channels: 'EMAIL', email: '', message: '', enabled: true }
  } else if (type === 'baseline') {
    expectTimeVal.value = '08:00:00'
    baseForm.value = { name: '', workflowId: '', bizDateMode: 'DAY', triggerType: 'DELAY', graceSeconds: 0, notifyChannels: 'EMAIL', operator: 'admin', enabled: true }
  }
  dialogVisible.value = true
}
function openEdit(row: AlarmEntity | AlarmBaselineEntity) {
  editing.value = row
  if (tab.value === 'config') {
    form.value = { ...(row as AlarmEntity) }
  } else {
    const b = row as AlarmBaselineEntity
    expectTimeVal.value = b.expectTime || '08:00:00'
    baseForm.value = { ...b }
  }
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (tab.value === 'config') {
      if (!form.value.name) {
        ElMessage.warning('名称必填')
        return
      }
      if (editing.value) await alarmApi.updateConfig(editing.value.id, form.value)
      else await alarmApi.createConfig(form.value)
    } else {
      if (!baseForm.value.name || !baseForm.value.workflowId) {
        ElMessage.warning('名称与工作流必填')
        return
      }
      const payload = { ...baseForm.value, expectTime: expectTimeVal.value }
      if (editing.value) await alarmApi.updateBaseline(editing.value.id, payload)
      else await alarmApi.createBaseline(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['alarm'] })
    if (tab.value === 'config') await loadConfig()
    else await loadBaselines()
  } finally {
    saving.value = false
  }
}

async function testConfig(row: AlarmEntity) {
  const res = await alarmApi.testConfig(row.id)
  ElMessage.success(`测试成功: ${JSON.stringify(res)?.slice(0, 80) || 'ok'}`)
}
async function testBaseline(row: AlarmBaselineEntity) {
  const res = await alarmApi.testBaseline(row.id)
  ElMessage.success(String(res || 'ok'))
}
async function toggleBaseline(row: AlarmBaselineEntity, v: boolean) {
  await alarmApi.toggleBaseline(row.id, v)
  ElMessage.success(v ? '已启用' : '已停用')
  await loadBaselines()
}
async function removeConfig(row: AlarmEntity) {
  await ElMessageBox.confirm(`确认删除告警规则 ${row.name} ？`, '提示', { type: 'warning' })
  await alarmApi.removeConfig(row.id)
  ElMessage.success('已删除')
  await loadConfig()
}
async function removeBaseline(row: AlarmBaselineEntity) {
  await ElMessageBox.confirm(`确认删除基线 ${row.name} ？`, '提示', { type: 'warning' })
  await alarmApi.removeBaseline(row.id)
  ElMessage.success('已删除')
  await loadBaselines()
}
async function ack(row: AlarmRecordEntity) {
  await alarmApi.ackRecord(row.id)
  ElMessage.success('已确认')
  await loadRecords()
}
async function close(row: AlarmRecordEntity) {
  await alarmApi.closeRecord(row.id)
  ElMessage.success('已关闭')
  await loadRecords()
}
async function runCheck() {
  checking.value = true
  try {
    const res = await alarmApi.runCheck()
    ElMessage.success(`基线检查完成: ${JSON.stringify(res)?.slice(0, 120) || 'ok'}`)
  } finally {
    checking.value = false
  }
}

function typeTag(t: string) {
  if (t === 'MISS' || t === 'FAIL') return 'danger'
  if (t === 'DELAY') return 'warning'
  return 'success'
}
</script>

<style scoped>
.page {
  padding: 24px;
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
</style>