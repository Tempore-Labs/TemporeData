<template>
  <div class="baseline-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">工作流基线告警</h2>
        <p class="page-desc">为工作流设定期望完成时间，运行时到点未完成自动生成告警记录</p>
      </div>
      <div class="header-actions">
        <el-button :icon="VideoPlay" @click="runCheck" :loading="checking">立即检查</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增基线</el-button>
      </div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="基线规则" name="baselines" />
      <el-tab-pane label="告警记录" name="records" />
    </el-tabs>

    <!-- 基线列表 -->
    <el-card v-if="tab === 'baselines'" shadow="never">
      <el-table :data="baselines" v-loading="loading" stripe>
        <el-table-column prop="name" label="基线名称" min-width="150" />
        <el-table-column prop="workflowId" label="工作流ID" min-width="190" show-overflow-tooltip />
        <el-table-column prop="expectTime" label="期望完成" width="100" />
        <el-table-column label="宽限" width="90">
          <template #default="{ row }">{{ row.graceSeconds ?? 300 }}s</template>
        </el-table-column>
        <el-table-column prop="bizDateMode" label="业务日期" width="100">
          <template #default="{ row }">{{ row.bizDateMode || 'DAY' }}</template>
        </el-table-column>
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" @change="v => toggleBaseline(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" link type="warning" @click="handleTest(row)">测试</el-button>
            <el-popconfirm title="删除该基线？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 告警记录 -->
    <el-card v-if="tab === 'records'" shadow="never">
      <div class="record-toolbar">
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 140px" @change="loadRecords">
          <el-option label="待确认" value="SENT" />
          <el-option label="已确认" value="ACK" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </div>
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="bizDate" label="业务日期" width="110" />
        <el-table-column prop="alarmType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="alarmTypeTag(row.alarmType)" size="small">{{ row.alarmType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expectFinishTime" label="期望完成" width="150" />
        <el-table-column prop="actualFinishTime" label="实际完成" width="150">
          <template #default="{ row }">{{ row.actualFinishTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="触发时间" min-width="150" />
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button size="small" link type="primary" :disabled="row.status === 'CLOSED'" @click="ackRecord(row)">确认</el-button>
            <el-button size="small" link type="danger" :disabled="row.status === 'CLOSED'" @click="closeRecord(row)">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑基线 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑基线' : '新增基线'" width="520px" :close-on-click-modal="false">
      <el-form label-width="110px">
        <el-form-item label="基线名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="工作流" required>
          <el-select v-model="form.workflowId" filterable style="width: 100%" placeholder="选择工作流">
            <el-option v-for="w in workflows" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="期望完成时间">
          <el-time-picker v-model="form.expectTime" format="HH:mm:ss" value-format="HH:mm:ss" placeholder="HH:mm:ss" />
        </el-form-item>
        <el-form-item label="宽限(秒)">
          <el-input-number v-model="form.graceSeconds" :min="0" :max="86400" :step="60" />
        </el-form-item>
        <el-form-item label="业务日期模式">
          <el-select v-model="form.bizDateMode" style="width: 100%">
            <el-option label="按天 (DAY)" value="DAY" />
            <el-option label="按周 (WEEK)" value="WEEK" />
            <el-option label="按月 (MONTH)" value="MONTH" />
          </el-select>
        </el-form-item>
        <el-form-item label="时区">
          <el-input v-model="form.timezone" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { baselineApi } from '@/api/modules/alarm'
import { workflowApi } from '@/api/modules/workflow'

const tab = ref('baselines')
const loading = ref(false)
const checking = ref(false)
const baselines = ref([])
const records = ref([])
const workflows = ref([])
const statusFilter = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const saving = ref(false)
const form = ref(emptyForm())

function emptyForm() {
  return {
    name: '', workflowId: '', expectTime: '06:00:00', graceSeconds: 300,
    bizDateMode: 'DAY', timezone: 'Asia/Shanghai', enabled: true
  }
}

function alarmTypeTag(t) {
  const map = { MISS: 'warning', DELAY: 'danger', FAIL: 'danger', RECOVERY: 'success' }
  return map[t] || 'info'
}
function statusTag(s) {
  const map = { PENDING: 'info', SENT: 'warning', ACK: 'primary', CLOSED: 'info' }
  return map[s] || 'info'
}

async function loadAll() {
  loading.value = true
  try {
    const [b, w] = await Promise.all([baselineApi.baselines(), workflowApi.list()])
    baselines.value = b || []
    workflows.value = w || []
  } catch (e) {
    ElMessage.error('加载失败：' + (e.message || ''))
  } finally { loading.value = false }
}

async function loadRecords() {
  loading.value = true
  try {
    records.value = await baselineApi.records(null, statusFilter.value || undefined) || []
  } catch (e) {
    ElMessage.error('加载告警记录失败')
  } finally { loading.value = false }
}

async function runCheck() {
  checking.value = true
  try {
    const res = await baselineApi.runCheck()
    ElMessage.success(`检查完成：MISS=${res.miss} DELAY=${res.delay} FAIL=${res.fail} RECOVERY=${res.recovery}`)
    if (tab.value === 'records') await loadRecords()
  } catch (e) {
    ElMessage.error('检查失败：' + (e.message || ''))
  } finally { checking.value = false }
}

function openAdd() {
  isEdit.value = false
  editId.value = null
  form.value = emptyForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.name || !form.value.workflowId) {
    ElMessage.warning('请填写名称并选择工作流')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) await baselineApi.update(editId.value, form.value)
    else await baselineApi.create(form.value)
    ElMessage.success('已保存')
    dialogVisible.value = false
    await loadAll()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally { saving.value = false }
}

async function toggleBaseline(row, v) {
  try {
    await baselineApi.toggle(row.id, v)
    row.enabled = v
  } catch (e) {
    ElMessage.error('切换失败')
  }
}

async function handleTest(row) {
  try {
    const msg = await baselineApi.test(row.id)
    ElMessage.success(msg)
  } catch (e) {
    ElMessage.error(e.message || '测试失败')
  }
}

async function handleDelete(id) {
  try {
    await baselineApi.delete(id)
    ElMessage.success('已删除')
    await loadAll()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

async function ackRecord(row) {
  try {
    await baselineApi.ack(row.id)
    await loadRecords()
  } catch (e) { ElMessage.error('确认失败') }
}

async function closeRecord(row) {
  try {
    await baselineApi.close(row.id)
    await loadRecords()
  } catch (e) { ElMessage.error('关闭失败') }
}

onMounted(async () => {
  await loadAll()
  await loadRecords()
})
</script>

<style scoped>
.baseline-page { max-width: 1280px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.page-desc { font-size: 13px; color: #9ca3af; margin: 4px 0 0; }
.header-actions { display: flex; gap: 8px; }
.record-toolbar { margin-bottom: 12px; }
</style>