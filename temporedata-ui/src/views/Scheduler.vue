<template>
  <div class="scheduler-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">调度中心</h2>
        <p class="page-desc">基于任务定义的定时调度引擎（P0-1）</p>
      </div>
      <div class="page-header-actions">
        <el-button @click="refreshAll">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="scheduler-tabs">
      <!-- ==================== 任务定义 ==================== -->
      <el-tab-pane label="任务定义" name="tasks">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="taskQuery" placeholder="按名称搜索..." :prefix-icon="Search" clearable style="width: 220px" />
          </div>
          <div class="toolbar-right">
            <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建任务</el-button>
          </div>
        </div>

        <div class="page-body">
          <el-table :data="filteredTasks" v-loading="loading" stripe>
            <el-table-column prop="name" label="任务名称" min-width="160" show-overflow-tooltip />
            <el-table-column label="类型" width="110" align="center">
              <template #default="{ row }">
                <el-tag effect="plain" size="small">{{ row.taskType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="targetRef" label="目标引用" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.targetRef || '-' }}</template>
            </el-table-column>
            <el-table-column prop="cronExpression" label="Cron 表达式" min-width="140" show-overflow-tooltip />
            <el-table-column label="启用" width="90" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.enabled"
                  @change="val => toggleEnabled(row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" align="center" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="handleTrigger(row)">
                  <el-icon><VideoPlay /></el-icon>立即执行
                </el-button>
                <el-button size="small" link @click="openEditDialog(row)">
                  <el-icon><EditPen /></el-icon>编辑
                </el-button>
                <el-button size="small" link @click="handleDelete(row)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ==================== 实例记录 ==================== -->
      <el-tab-pane name="instances">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-select
              v-model="instanceTaskId"
              placeholder="按任务筛选"
              clearable
              style="width: 260px"
              @change="loadInstances"
            >
              <el-option v-for="t in tasks" :key="t.id" :label="`${t.name} (${t.taskType})`" :value="t.id" />
            </el-select>
            <el-tag v-if="instances.length" type="info" effect="plain">{{ instances.length }} 条</el-tag>
          </div>
        </div>

        <div class="page-body">
          <el-table :data="pagedInstances" v-loading="instLoading" stripe>
            <el-table-column prop="id" label="实例 ID" min-width="180" show-overflow-tooltip />
            <el-table-column prop="instanceNo" label="序号" width="80" align="center" />
            <el-table-column prop="bizDate" label="业务日期" width="110" align="center">
              <template #default="{ row }">{{ row.bizDate || '-' }}</template>
            </el-table-column>
            <el-table-column prop="triggerTime" label="触发时间" min-width="160" />
            <el-table-column prop="startTime" label="开始时间" min-width="160">
              <template #default="{ row }">{{ row.startTime || '-' }}</template>
            </el-table-column>
            <el-table-column prop="finishTime" label="结束时间" min-width="160">
              <template #default="{ row }">{{ row.finishTime || '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="resultMsg" label="结果" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.resultMsg || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button size="small" link type="primary" @click="openLogs(row)">
                  <el-icon><Tickets /></el-icon>日志
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="instancePage"
              v-model:page-size="instancePageSize"
              :page-sizes="[10, 20, 50]"
              :total="instances.length"
              layout="total, sizes, prev, pager, next, jumper"
              background
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ==================== 任务编辑弹窗 ==================== -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑任务' : '新建任务'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" label-width="120px" label-position="right">
        <el-form-item label="任务名称" required>
          <el-input v-model="form.name" placeholder="例如：每日报表任务" />
        </el-form-item>
        <el-form-item label="任务类型" required>
          <el-select v-model="form.taskType" style="width: 100%">
            <el-option label="工作流 WORKFLOW" value="WORKFLOW" />
            <el-option label="SQL" value="SQL" />
            <el-option label="HTTP" value="HTTP" />
            <el-option label="SHELL" value="SHELL" />
            <el-option label="质量检查 QUALITY" value="QUALITY" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.taskType === 'WORKFLOW'" label="目标工作流 ID" required>
          <el-input v-model="form.targetRef" :placeholder="editingId ? '留空保持不变' : '粘贴工作流 ID'" />
        </el-form-item>
        <el-form-item v-else label="目标引用">
          <el-input v-model="form.targetRef" placeholder="SQL ID / 接口 ID / 脚本路径" />
        </el-form-item>
        <el-form-item label="Cron 表达式" required>
          <el-input v-model="form.cronExpression" placeholder="例如：0 0 8 * * ?" />
        </el-form-item>
        <el-form-item label="Cron 预置">
          <div class="cron-presets">
            <el-tag
              v-for="preset in cronPresets"
              :key="preset.label"
              :type="form.cronExpression === preset.value ? 'primary' : 'info'"
              effect="plain"
              class="cron-preset-tag"
              @click="form.cronExpression = preset.value"
            >
              {{ preset.label }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="时区">
          <el-input v-model="form.timezone" placeholder="Asia/Shanghai" />
        </el-form-item>
        <el-form-item label="业务日期模式">
          <el-select v-model="form.bizDateMode" style="width: 100%">
            <el-option label="不启用 (NONE)" value="NONE" />
            <el-option label="按天切日 (DAY)" value="DAY" />
            <el-option label="按周切日 (WEEK)" value="WEEK" />
            <el-option label="按月切日 (MONTH)" value="MONTH" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务日历" v-if="form.bizDateMode !== 'NONE'">
          <el-select v-model="form.calendarId" style="width: 100%" clearable placeholder="选择自定义日历（可选）">
            <el-option v-for="c in calendars" :key="c.id" :label="`${c.name}（切日 ${cutLabel(c)}）`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.owner" placeholder="可选" />
        </el-form-item>
        <el-form-item label="启用任务">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 实例日志弹窗 ==================== -->
    <el-dialog v-model="logVisible" :title="`实例日志 - ${currentInstance?.id || ''}`" width="640px">
      <pre v-loading="logLoading" class="log-pre">{{ logText || '暂无日志' }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Refresh, Search, VideoPlay, EditPen, Delete, Tickets } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { schedulerApi } from '@/api/modules/scheduler'
import { calendarApi } from '@/api/modules/calendar'

const activeTab = ref('tasks')

// ---- 任务定义 ----
const tasks = ref([])
const loading = ref(false)
const taskQuery = ref('')
const calendars = ref([])

function cutLabel(c) {
  const h = c.cutHour ?? 0
  const m = c.cutMinute ?? 0
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

async function fetchCalendars() {
  try {
    calendars.value = await calendarApi.list() || []
  } catch { /* ignore */ }
}

const filteredTasks = computed(() => {
  const kw = taskQuery.value.trim().toLowerCase()
  if (!kw) return tasks.value
  return tasks.value.filter(t => (t.name || '').toLowerCase().includes(kw))
})

async function fetchTasks() {
  loading.value = true
  try {
    tasks.value = await schedulerApi.tasks()
  } catch {
    ElMessage.error('加载任务失败')
  } finally {
    loading.value = false
  }
}

// ---- 新建/编辑 ----
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const emptyForm = () => ({
  name: '',
  taskType: 'WORKFLOW',
  targetRef: '',
  cronExpression: '0 0 8 * * ?',
  timezone: 'Asia/Shanghai',
  bizDateMode: 'NONE',
  calendarId: '',
  owner: '',
  enabled: true
})
const form = ref(emptyForm())

const cronPresets = [
  { label: '每小时', value: '0 0 * * * ?' },
  { label: '每天 8 点', value: '0 0 8 * * ?' },
  { label: '每天 0 点', value: '0 0 0 * * ?' },
  { label: '每周一 0 点', value: '0 0 0 ? * MON' },
  { label: '每月 1 号 0 点', value: '0 0 0 1 * ?' }
]

function openCreateDialog() {
  editingId.value = null
  form.value = emptyForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  editingId.value = row.id
  form.value = {
    name: row.name,
    taskType: row.taskType,
    targetRef: row.targetRef,
    cronExpression: row.cronExpression,
    timezone: row.timezone || 'Asia/Shanghai',
    bizDateMode: row.bizDateMode || 'NONE',
    calendarId: row.calendarId || '',
    owner: row.owner,
    enabled: row.enabled
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.name || !form.value.cronExpression) {
    ElMessage.warning('请填写任务名称和 Cron 表达式')
    return
  }
  if (form.value.taskType === 'WORKFLOW' && !form.value.targetRef && !editingId.value) {
    ElMessage.warning('请填写目标工作流 ID')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await schedulerApi.updateTask(editingId.value, form.value)
      ElMessage.success('任务已更新')
    } else {
      await schedulerApi.createTask(form.value)
      ElMessage.success('任务已创建')
    }
    dialogVisible.value = false
    await fetchTasks()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除任务「${row.name}」？`, '删除任务', { type: 'warning' })
  } catch {
    return
  }
  try {
    await schedulerApi.deleteTask(row.id)
    ElMessage.success('任务已删除')
    await fetchTasks()
  } catch {
    ElMessage.error('删除失败')
  }
}

async function toggleEnabled(row, val) {
  try {
    await schedulerApi.setEnabled(row.id, val)
    ElMessage.success(val ? '任务已启用' : '任务已停用')
    row.enabled = val
  } catch {
    ElMessage.error('操作失败')
  }
}

// ---- 立即执行 / 实例 ----
const instanceTaskId = ref(null)
const instances = ref([])
const instLoading = ref(false)
const instancePage = ref(1)
const instancePageSize = ref(10)

const pagedInstances = computed(() => {
  const s = (instancePage.value - 1) * instancePageSize.value
  return instances.value.slice(s, s + instancePageSize.value)
})

async function handleTrigger(row) {
  try {
    await schedulerApi.trigger(row.id)
    ElMessage.success('已触发执行')
    if (instanceTaskId.value === row.id) await loadInstances()
  } catch (e) {
    ElMessage.error(e.message || '触发失败')
  }
}

async function loadInstances() {
  if (!instanceTaskId.value) {
    instances.value = []
    return
  }
  instLoading.value = true
  try {
    instances.value = await schedulerApi.instances(instanceTaskId.value)
  } catch {
    ElMessage.error('加载实例失败')
  } finally {
    instLoading.value = false
  }
}

// ---- 日志 ----
const logVisible = ref(false)
const logLoading = ref(false)
const logText = ref('')
const currentInstance = ref(null)

async function openLogs(row) {
  currentInstance.value = row
  logVisible.value = true
  logLoading.value = true
  logText.value = ''
  try {
    const list = await schedulerApi.logs(row.id)
    logText.value = list.map(l => `[${l.createTime}] [${l.level}] ${l.message}`).join('\n')
  } catch {
    logText.value = '加载日志失败'
  } finally {
    logLoading.value = false
  }
}

function statusTag(status) {
  const map = { SUCCESS: 'success', RUNNING: 'warning', PENDING: 'info', FAILED: 'danger', TIMEOUT: 'danger', SKIPPED: 'info', CANCELED: 'info' }
  return map[status] || 'info'
}

function refreshAll() {
  fetchTasks()
  if (instanceTaskId.value) loadInstances()
}

onMounted(() => {
  fetchTasks()
  fetchCalendars()
})
</script>

<style scoped>
.scheduler-page {
  max-width: 1360px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.page-desc {
  font-size: 13px;
  color: #9ca3af;
  margin: 4px 0 0;
}

.page-header-actions {
  display: flex;
  gap: 8px;
}

.scheduler-tabs {
  background: #fff;
  border-radius: 8px;
  padding: 0 20px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 12px 0;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  gap: 8px;
  align-items: center;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.page-body {
  background: #fff;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.cron-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.cron-preset-tag {
  cursor: pointer;
  user-select: none;
}

.log-pre {
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  line-height: 1.6;
  background: #0f172a;
  color: #e2e8f0;
  padding: 14px;
  border-radius: 8px;
  max-height: 420px;
  overflow-y: auto;
}
</style>