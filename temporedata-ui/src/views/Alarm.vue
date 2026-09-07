<template>
  <div class="alarm-page">
    <div class="page-header">
      <h2 class="page-title">告警配置</h2>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">新增告警</el-button>
    </div>

    <div class="page-body">
      <el-table :data="pagedData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="告警名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="eventType" label="事件类型" width="140">
          <template #default="{ row }">
            <el-tag :type="eventTypeTag(row.eventType)" effect="light">{{ row.eventType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通知渠道" width="200">
          <template #default="{ row }">
            <el-tag v-for="ch in parseChannels(row.channels)" :key="ch" size="small" style="margin-right: 4px;">
              {{ ch }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="启用状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
              <el-button type="warning" link :icon="VideoPlay" @click="handleTest(row)">测试</el-button>
              <el-button type="info" link :icon="Tickets" @click="openRecordsDialog(row)">记录</el-button>
              <el-popconfirm title="确定要删除该告警配置吗？" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button type="danger" link :icon="Delete">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="tableData.length"
          layout="total, sizes, prev, pager, next"
          @size-change="currentPage = 1"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑告警配置' : '新增告警配置'"
      width="540px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="right">
        <el-form-item label="告警名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入告警名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="事件类型" prop="eventType">
          <el-select v-model="form.eventType" placeholder="请选择事件类型" style="width: 100%">
            <el-option label="任务失败" value="TASK_FAILURE" />
            <el-option label="任务超时" value="TASK_TIMEOUT" />
            <el-option label="数据延迟" value="DATA_DELAY" />
            <el-option label="阈值告警" value="THRESHOLD" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知渠道" prop="channels">
          <el-checkbox-group v-model="form.channels">
            <el-checkbox label="邮件" value="EMAIL" />
            <el-checkbox label="Webhook" value="WEBHOOK" />
            <el-checkbox label="短信" value="SMS" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="邮箱地址" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱地址，多个用逗号分隔" />
        </el-form-item>
        <el-form-item label="Webhook URL" prop="webhookUrl">
          <el-input v-model="form.webhookUrl" placeholder="请输入 Webhook URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 告警记录弹窗 -->
    <el-dialog
      v-model="recordsDialogVisible"
      :title="`告警记录 - ${currentConfig?.name || ''}`"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-table :data="recordsData" v-loading="recordsLoading" stripe style="width: 100%">
        <el-table-column prop="eventType" label="事件类型" width="120" />
        <el-table-column prop="message" label="告警内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SENT' ? 'success' : 'warning'" size="small">
              {{ row.status === 'SENT' ? '已发送' : '待发送' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
      </el-table>
      <template #footer>
        <el-button @click="recordsDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, VideoPlay, Tickets } from '@element-plus/icons-vue'
import { alarmApi } from '@/api/modules/alarm'
import { ElMessage } from 'element-plus'

// ---- 表格数据 ----
const tableData = ref([])
const loading = ref(false)

// ---- 分页 ----
const currentPage = ref(1)
const pageSize = ref(10)

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return tableData.value.slice(start, start + pageSize.value)
})

// ---- 弹窗 ----
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)

const defaultForm = () => ({
  name: '',
  eventType: '',
  channels: [],
  email: '',
  webhookUrl: ''
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入告警名称', trigger: 'blur' }],
  eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
  channels: [{ required: true, message: '请选择至少一个通知渠道', trigger: 'change' }]
}

// ---- 告警记录 ----
const recordsDialogVisible = ref(false)
const currentConfig = ref(null)
const recordsData = ref([])
const recordsLoading = ref(false)

// ---- 工具函数 ----
function eventTypeTag(type) {
  const map = { TASK_FAILURE: 'danger', TASK_TIMEOUT: 'warning', DATA_DELAY: 'warning', THRESHOLD: '' }
  return map[type] || 'info'
}

function parseChannels(channels) {
  if (!channels) return []
  if (Array.isArray(channels)) return channels
  return channels.split(',').map(c => c.trim())
}

function levelTag(level) {
  const map = { CRITICAL: 'danger', WARNING: 'warning', INFO: 'info' }
  return map[level] || 'info'
}

// ---- 加载数据 ----
async function fetchData() {
  loading.value = true
  try {
    tableData.value = await alarmApi.listConfigs()
  } catch (err) {
    ElMessage.error(err.message || '加载告警配置列表失败')
  } finally {
    loading.value = false
  }
}

// ---- 新增 ----
function openAddDialog() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

// ---- 编辑 ----
function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    name: row.name,
    eventType: row.eventType,
    channels: parseChannels(row.channels),
    email: row.email || '',
    webhookUrl: row.webhookUrl || ''
  })
  dialogVisible.value = true
}

// ---- 提交 ----
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = {
      ...form,
      channels: form.channels.join(',')
    }
    if (isEdit.value) {
      await alarmApi.updateConfig(editId.value, payload)
      ElMessage.success('告警配置更新成功')
    } else {
      await alarmApi.createConfig(payload)
      ElMessage.success('告警配置创建成功')
    }
    dialogVisible.value = false
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// ---- 删除 ----
async function handleDelete(id) {
  try {
    await alarmApi.deleteConfig(id)
    ElMessage.success('告警配置已删除')
    if (pagedData.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

// ---- 测试 ----
async function handleTest(row) {
  try {
    await alarmApi.testAlarm(row.id)
    ElMessage.success('告警测试已发送')
  } catch (err) {
    ElMessage.error(err.message || '告警测试失败')
  }
}

// ---- 查看记录 ----
async function openRecordsDialog(row) {
  currentConfig.value = row
  recordsDialogVisible.value = true
  await fetchRecords()
}

async function fetchRecords() {
  recordsLoading.value = true
  try {
    recordsData.value = await alarmApi.listRecords(currentConfig.value.id)
  } catch (err) {
    ElMessage.error(err.message || '加载告警记录失败')
  } finally {
    recordsLoading.value = false
  }
}

// ---- 初始化 ----
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.alarm-page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.page-body {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>