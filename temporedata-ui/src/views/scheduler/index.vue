<template>
  <div class="page">
    <PageHeader title="调度中心" subtitle="任务定义、定时触发、业务日期预览与批量补数">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建任务</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="name" label="任务名称" min-width="150">
        <template #default="{ row }"><span class="mono">{{ row.name }}</span></template>
      </el-table-column>
      <el-table-column prop="taskType" label="类型" width="100">
        <template #default="{ row }"><el-tag size="small">{{ row.taskType || '—' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="targetRef" label="目标" min-width="150">
        <template #default="{ row }"><span class="mono">{{ row.targetRef || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="bizDateMode" label="业务日期" width="100">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.bizDateMode || 'NONE' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="cronExpression" label="Cron" min-width="120">
        <template #default="{ row }"><span class="mono">{{ row.cronExpression || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-switch :model-value="!!row.enabled" @change="(v: boolean) => toggleEnabled(row, v)" />
        </template>
      </el-table-column>
      <el-table-column prop="owner" label="Owner" width="90" />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="success" @click="trigger(row)">触发</el-button>
          <el-button size="small" text type="primary" @click="openBackfill(row)">补数</el-button>
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="info" @click="viewInstances(row)">实例</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <!-- Create/edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑任务' : '新建任务'" width="540px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如 hourly_order_sync" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.taskType" style="width: 100%">
            <el-option v-for="t in ['SQL', 'SYNC', 'WORKFLOW', 'SCRIPT']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标">
          <el-input v-model="form.targetRef" placeholder="如 sql:SELECT 1" />
        </el-form-item>
        <el-form-item label="Cron" required>
          <el-input v-model="form.cronExpression" placeholder="QUARTZ 6 段，如 0 0 8 * * ?（每天 8 点）" />
          <div class="field-hint">6 段格式：秒 分 时 日 月 周。示例：<code>0 0 8 * * ?</code>、<code>0 0 * * * ?</code></div>
        </el-form-item>
        <el-form-item label="业务日期">
          <el-select v-model="form.bizDateMode" style="width: 100%">
            <el-option v-for="m in ['NONE', 'DAY', 'WEEK', 'MONTH']" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="时区">
          <el-input v-model="form.timezone" placeholder="Asia/Shanghai" />
        </el-form-item>
        <el-form-item label="参数">
          <el-input v-model="form.params" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="Owner">
          <el-input v-model="form.owner" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- Backfill dialog -->
    <el-dialog v-model="backfillVisible" :title="`批量补数 · ${backfillTargetName}`" width="560px">
      <el-form label-width="90px">
        <el-form-item label="补数区间" required>
          <el-date-picker
            v-model="backfillRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="业务日期模式">
          <el-tag size="small">{{ backfillTarget?.bizDateMode || 'NONE' }}</el-tag>
          <el-button v-if="backfillTarget?.bizDateMode === 'NONE'" size="small" text type="warning" @click="promoteBizDateMode">
            无模式，点击设为 DAY 以便补数
          </el-button>
        </el-form-item>
        <el-form-item label="日期预览">
          <el-button size="small" :loading="previewLoading" @click="previewBizDates">预览业务日期</el-button>
        </el-form-item>
      </el-form>

      <div v-if="bizPreview.length" class="preview-box">
        <div class="preview-title">将生成以下业务日期的实例：</div>
        <div class="preview-list">
          <el-tag
            v-for="it in bizPreview"
            :key="it.bizDate"
            size="small"
            :type="it.workday ? 'success' : 'info'"
            effect="plain"
            class="preview-chip"
          >
            {{ it.bizDate }}{{ it.workday ? '' : ' (非工作日)' }}
          </el-tag>
        </div>
      </div>

      <template #footer>
        <el-button @click="backfillVisible = false">取消</el-button>
        <el-button type="primary" :loading="backfilling" :disabled="!backfillRange" @click="submitBackfill">
          提交补数
        </el-button>
      </template>
    </el-dialog>

    <!-- Instances dialog -->
    <el-dialog v-model="instanceVisible" :title="`实例列表 · ${instanceTargetName}`" width="760px">
      <el-skeleton v-if="instanceLoading" :rows="5" animated />
      <DataTable v-else :data="instances">
        <el-table-column prop="instanceNo" label="# 序号" width="80" align="center">
          <template #default="{ row }"><span class="mono">{{ row.instanceNo }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="bizDate" label="业务日期" width="110">
          <template #default="{ row }"><span class="mono">{{ row.bizDate || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="140">
          <template #default="{ row }"><span class="mono muted">{{ row.startTime || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="finishTime" label="结束时间" min-width="140">
          <template #default="{ row }"><span class="mono muted">{{ row.finishTime || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="resultMsg" label="结果" min-width="120">
          <template #default="{ row }">
            <span class="mono muted ellipsis" :title="row.resultMsg || ''">{{ row.resultMsg || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="日志" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="viewLogs(row)">日志</el-button>
          </template>
        </el-table-column>
      </DataTable>
    </el-dialog>

    <!-- Instance logs drawer -->
    <el-drawer v-model="logVisible" :title="`实例日志 · ${logInstanceLabel}`" size="46%">
      <el-skeleton v-if="logLoading" :rows="8" animated />
      <div v-else class="log-box">
        <div v-if="!logs.length" class="muted">暂无日志</div>
        <div v-for="(l, i) in logs" :key="i" class="log-line">
          <span class="mono time">{{ l.createTime || '' }}</span>
          <span :class="['mono', 'level', l.level === 'ERROR' ? 'err' : '']">[{{ l.level }}]</span>
          <span class="msg">{{ l.message || '' }}</span>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import {
  schedulerApi,
  type TaskDefineReq,
  type TaskDefineRes,
  type BizDateItem,
  type TaskLogItem,
} from '@/api/scheduler'

const queryClient = useQueryClient()
const rows = ref<TaskDefineRes[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const editing = ref<TaskDefineRes | null>(null)
const saving = ref(false)
const form = ref<TaskDefineReq>({ name: '' })

// Instances
const instanceVisible = ref(false)
const instanceLoading = ref(false)
const instanceTargetName = ref('')
const instances = ref<any[]>([])

// Logs
const logVisible = ref(false)
const logLoading = ref(false)
const logInstanceLabel = ref('')
const logs = ref<TaskLogItem[]>([])

// Backfill
const backfillVisible = ref(false)
const backfillTarget = ref<TaskDefineRes | null>(null)
const backfillTargetName = ref('')
const backfillRange = ref<[string, string] | null>(null)
const bizPreview = ref<BizDateItem[]>([])
const previewLoading = ref(false)
const backfilling = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = await schedulerApi.tasks()
  } finally {
    loading.value = false
  }
}
load()

function openCreate() {
  editing.value = null
  form.value = {
    name: '',
    taskType: 'SQL',
    targetRef: '',
    cronExpression: '0 0 8 * * ?',
    bizDateMode: 'DAY',
    timezone: 'Asia/Shanghai',
    enabled: true,
    owner: 'admin',
  }
  dialogVisible.value = true
}
function openEdit(row: TaskDefineRes) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.name) {
    ElMessage.warning('名称必填')
    return
  }
  if (!form.value.cronExpression) {
    ElMessage.warning('Cron 必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) await schedulerApi.update(editing.value.id, form.value)
    else await schedulerApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['scheduler'] })
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(row: TaskDefineRes, v: boolean) {
  await schedulerApi.setEnabled(row.id, v)
  ElMessage.success(v ? '已启用' : '已停用')
  await load()
}

async function trigger(row: TaskDefineRes) {
  const res = await schedulerApi.trigger(row.id)
  ElMessage.success(`已触发实例 #${res?.instanceNo ?? ''} ${res?.status ?? ''}`.trim())
}

async function viewInstances(row: TaskDefineRes) {
  instanceTargetName.value = row.name
  instanceVisible.value = true
  instanceLoading.value = true
  try {
    instances.value = await schedulerApi.instances(row.id)
  } finally {
    instanceLoading.value = false
  }
}

async function viewLogs(row: any) {
  logVisible.value = true
  logLoading.value = true
  logInstanceLabel.value = `#${row.instanceNo ?? row.id}`
  logs.value = []
  try {
    logs.value = await schedulerApi.logs(row.id)
  } finally {
    logLoading.value = false
  }
}

// Backfill
function openBackfill(row: TaskDefineRes) {
  backfillTarget.value = row
  backfillTargetName.value = row.name
  backfillRange.value = null
  bizPreview.value = []
  backfillVisible.value = true
}

function promoteBizDateMode() {
  if (!backfillTarget.value) return
  const { id: _id, ...rest } = backfillTarget.value
  schedulerApi.update(backfillTarget.value.id, { ...rest, name: rest.name || '', bizDateMode: 'DAY' }).then((updated) => {
    backfillTarget.value = updated
    ElMessage.success('已设为 DAY 模式，可进行补数')
    load()
  })
}

async function previewBizDates() {
  if (!backfillTarget.value || !backfillRange.value) {
    ElMessage.warning('请先选择补数区间')
    return
  }
  previewLoading.value = true
  try {
    const [from, to] = backfillRange.value
    bizPreview.value = await schedulerApi.bizDates(backfillTarget.value.id, from, to)
    if (!bizPreview.value.length) ElMessage.info('该区间没有可生成的业务日期')
  } finally {
    previewLoading.value = false
  }
}

async function submitBackfill() {
  if (!backfillTarget.value || !backfillRange.value) return
  if (backfillTarget.value.bizDateMode === 'NONE') {
    ElMessage.warning('该任务业务日期模式为 NONE，无法补数，请先设为 DAY/WEEK/MONTH')
    return
  }
  backfilling.value = true
  try {
    const [from, to] = backfillRange.value
    const count = await schedulerApi.backfill(backfillTarget.value.id, from, to)
    ElMessage.success(`补数完成，共生成 ${count} 个实例`)
    backfillVisible.value = false
  } finally {
    backfilling.value = false
  }
}

async function remove(row: TaskDefineRes) {
  await ElMessageBox.confirm(`确认删除任务 ${row.name} ？`, '提示', { type: 'warning' })
  await schedulerApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.mono {
  font-family: var(--td-font-mono);
}
.muted {
  color: var(--td-text-4);
}
.ellipsis {
  display: inline-block;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
.field-hint {
  font-size: 12px;
  color: var(--td-text-4);
  line-height: 1.5;
  margin-top: 2px;
}
.field-hint code {
  font-family: var(--td-font-mono);
  background: var(--td-bg-2);
  padding: 0 4px;
  border-radius: 4px;
}
.preview-box {
  margin-top: 4px;
  border: 1px solid var(--td-border);
  border-radius: 8px;
  padding: 12px;
}
.preview-title {
  font-size: 13px;
  color: var(--td-text-2);
  margin-bottom: 8px;
}
.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.log-box {
  font-size: 12px;
}
.log-line {
  padding: 3px 0;
  border-bottom: 1px solid var(--td-bg-2);
  white-space: pre-wrap;
}
.log-line .time {
  color: var(--td-text-4);
  margin-right: 8px;
}
.log-line .level {
  color: #909399;
  margin-right: 8px;
}
.log-line .level.err {
  color: #f56c6c;
  font-weight: 600;
}
</style>