<template>
  <div class="sync-task-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>数据同步任务</h2>
      <div class="page-header-actions">
        <el-button @click="fetchData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon>
          新增同步任务
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table :data="pagedData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="任务名称" min-width="160" />
        <el-table-column prop="sourceType" label="源端类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.sourceType)" effect="light">{{ row.sourceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceDb" label="源端数据库" min-width="140" />
        <el-table-column prop="targetType" label="目标端类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.targetType)" effect="light">{{ row.targetType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetDb" label="目标端数据库" min-width="140" />
        <el-table-column label="同步模式" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.syncMode === 'FULL' ? 'primary' : 'warning'" effect="light">
              {{ row.syncMode === 'FULL' ? '全量' : '增量' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" effect="dark">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openEditDialog(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button size="small" type="success" link @click="handleExecute(row)">
              <el-icon><VideoPlay /></el-icon>
              执行
            </el-button>
            <el-popconfirm title="确定删除该同步任务吗？" confirm-button-text="确定" cancel-button-text="取消" @confirm="handleDelete(row)">
              <template #reference>
                <el-button size="small" type="danger" link>
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="tableData.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑同步任务' : '新增同步任务'"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" label-position="right">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入任务名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="源端类型" prop="sourceType">
          <el-select v-model="form.sourceType" placeholder="请选择源端类型" style="width: 100%">
            <el-option label="MySQL" value="MySQL" />
            <el-option label="PostgreSQL" value="PostgreSQL" />
            <el-option label="Hive" value="Hive" />
          </el-select>
        </el-form-item>
        <el-form-item label="源端数据库" prop="sourceDb">
          <el-input v-model="form.sourceDb" placeholder="请输入源端数据库名" />
        </el-form-item>
        <el-form-item label="目标端类型" prop="targetType">
          <el-select v-model="form.targetType" placeholder="请选择目标端类型" style="width: 100%">
            <el-option label="MySQL" value="MySQL" />
            <el-option label="PostgreSQL" value="PostgreSQL" />
            <el-option label="Hive" value="Hive" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标端数据库" prop="targetDb">
          <el-input v-model="form.targetDb" placeholder="请输入目标端数据库名" />
        </el-form-item>
        <el-form-item label="同步模式" prop="syncMode">
          <el-select v-model="form.syncMode" placeholder="请选择同步模式" style="width: 100%">
            <el-option label="全量同步" value="FULL" />
            <el-option label="增量同步" value="INCREMENTAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入任务描述" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { syncApi } from '@/api/modules/sync'
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
  sourceType: '',
  sourceDb: '',
  targetType: '',
  targetDb: '',
  syncMode: '',
  description: ''
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择源端类型', trigger: 'change' }],
  sourceDb: [{ required: true, message: '请输入源端数据库', trigger: 'blur' }],
  targetType: [{ required: true, message: '请选择目标端类型', trigger: 'change' }],
  targetDb: [{ required: true, message: '请输入目标端数据库', trigger: 'blur' }],
  syncMode: [{ required: true, message: '请选择同步模式', trigger: 'change' }]
}

// ---- 类型标签颜色 ----
function typeTag(type) {
  const map = { MySQL: '', PostgreSQL: 'success', Hive: 'warning' }
  return map[type] || 'info'
}

// ---- 状态标签颜色与文案 ----
function statusTag(status) {
  const map = { PENDING: 'info', RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { PENDING: '待执行', RUNNING: '运行中', SUCCESS: '成功', FAILED: '失败' }
  return map[status] || status
}

// ---- 加载数据 ----
async function fetchData() {
  loading.value = true
  try {
    tableData.value = await syncApi.list()
  } catch (err) {
    ElMessage.error(err.message || '加载同步任务列表失败')
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
    sourceType: row.sourceType,
    sourceDb: row.sourceDb,
    targetType: row.targetType,
    targetDb: row.targetDb,
    syncMode: row.syncMode,
    description: row.description || ''
  })
  dialogVisible.value = true
}

// ---- 提交 ----
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = { ...form }
    if (isEdit.value) {
      await syncApi.update(editId.value, payload)
      ElMessage.success('同步任务更新成功')
    } else {
      await syncApi.create(payload)
      ElMessage.success('同步任务创建成功')
    }
    dialogVisible.value = false
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// ---- 执行 ----
async function handleExecute(row) {
  try {
    await syncApi.execute(row.id)
    ElMessage.success('同步任务已触发执行')
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '执行失败')
  }
}

// ---- 删除 ----
async function handleDelete(row) {
  try {
    await syncApi.delete(row.id)
    ElMessage.success('同步任务已删除')
    const remaining = tableData.value.length - 1
    if (pagedData.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

// ---- 初始化 ----
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.sync-task-page {
  padding: 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.page-header-actions {
  display: flex;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>