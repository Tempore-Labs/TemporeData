<template>
  <div class="cluster-page">
    <div class="page-header">
      <h2 class="page-title">计算集群</h2>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">新增集群</el-button>
    </div>

    <div class="page-body">
      <el-table :data="pagedData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="集群名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="clusterTypeTag(row.type)" effect="light">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="masterUrl" label="Master URL" min-width="240" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nodeCount" label="节点数" width="90" align="center" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
              <el-button type="success" link :icon="Monitor" @click="openNodesDialog(row)">节点</el-button>
              <el-popconfirm title="确定要删除该集群吗？" @confirm="handleDelete(row.id)">
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
      :title="isEdit ? '编辑集群' : '新增集群'"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="right">
        <el-form-item label="集群名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入集群名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择集群类型" style="width: 100%">
            <el-option label="Spark Standalone" value="Spark Standalone" />
            <el-option label="YARN" value="YARN" />
            <el-option label="Kubernetes" value="Kubernetes" />
          </el-select>
        </el-form-item>
        <el-form-item label="Master URL" prop="masterUrl">
          <el-input v-model="form.masterUrl" placeholder="请输入 Master URL" maxlength="256" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 节点管理弹窗 -->
    <el-dialog
      v-model="nodesDialogVisible"
      :title="`节点管理 - ${currentCluster?.name || ''}`"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="nodes-toolbar">
        <el-button type="primary" size="small" :icon="Plus" @click="openAddNodeForm">添加节点</el-button>
      </div>
      <el-table :data="nodesData" v-loading="nodesLoading" stripe style="width: 100%">
        <el-table-column prop="host" label="主机地址" min-width="160" />
        <el-table-column prop="port" label="端口" width="100" align="center" />
        <el-table-column prop="agentStatus" label="Agent 状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.agentStatus === 'RUNNING' ? 'success' : 'danger'" size="small">
              {{ row.agentStatus === 'RUNNING' ? '运行中' : '未安装' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button size="small" type="primary" link :icon="Upload" @click="handleInstallAgent(row)">安装Agent</el-button>
              <el-button size="small" type="warning" link :icon="CircleCheck" @click="handleCheckStatus(row)">检查状态</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="nodesDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 添加节点弹窗 -->
    <el-dialog
      v-model="addNodeDialogVisible"
      title="添加节点"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="nodeFormRef" :model="nodeForm" :rules="nodeRules" label-width="100px" label-position="right">
        <el-form-item label="主机地址" prop="host">
          <el-input v-model="nodeForm.host" placeholder="请输入主机地址" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="nodeForm.port" :min="1" :max="65535" placeholder="请输入端口" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addNodeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addNodeLoading" @click="handleAddNodeSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, Monitor, Upload, CircleCheck } from '@element-plus/icons-vue'
import { clusterApi } from '@/api/modules/cluster'
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
  type: '',
  masterUrl: ''
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入集群名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择集群类型', trigger: 'change' }],
  masterUrl: [{ required: true, message: '请输入 Master URL', trigger: 'blur' }]
}

// ---- 节点管理 ----
const nodesDialogVisible = ref(false)
const currentCluster = ref(null)
const nodesData = ref([])
const nodesLoading = ref(false)

// ---- 添加节点 ----
const addNodeDialogVisible = ref(false)
const addNodeLoading = ref(false)
const nodeFormRef = ref(null)

const nodeForm = reactive({
  host: '',
  port: 22
})

const nodeRules = {
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }]
}

// ---- 工具函数 ----
function clusterTypeTag(type) {
  const map = { 'Spark Standalone': '', 'YARN': 'warning', 'Kubernetes': 'success' }
  return map[type] || 'info'
}

function statusTagType(status) {
  const map = { READY: 'success', OFFLINE: 'danger', STARTING: 'warning' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { READY: '就绪', OFFLINE: '离线', STARTING: '启动中' }
  return map[status] || status
}

// ---- 加载数据 ----
async function fetchData() {
  loading.value = true
  try {
    tableData.value = await clusterApi.list()
  } catch (err) {
    ElMessage.error(err.message || '加载集群列表失败')
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
    type: row.type,
    masterUrl: row.masterUrl
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
      await clusterApi.update(editId.value, payload)
      ElMessage.success('集群更新成功')
    } else {
      await clusterApi.create(payload)
      ElMessage.success('集群创建成功')
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
    await clusterApi.delete(id)
    ElMessage.success('集群已删除')
    if (pagedData.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

// ---- 节点管理 ----
async function openNodesDialog(row) {
  currentCluster.value = row
  nodesDialogVisible.value = true
  await fetchNodes()
}

async function fetchNodes() {
  nodesLoading.value = true
  try {
    nodesData.value = await clusterApi.listNodes(currentCluster.value.id)
  } catch (err) {
    ElMessage.error(err.message || '加载节点列表失败')
  } finally {
    nodesLoading.value = false
  }
}

function openAddNodeForm() {
  nodeForm.host = ''
  nodeForm.port = 22
  addNodeDialogVisible.value = true
}

async function handleAddNodeSubmit() {
  const valid = await nodeFormRef.value.validate().catch(() => false)
  if (!valid) return

  addNodeLoading.value = true
  try {
    await clusterApi.addNode(currentCluster.value.id, { ...nodeForm })
    ElMessage.success('节点添加成功')
    addNodeDialogVisible.value = false
    await fetchNodes()
  } catch (err) {
    ElMessage.error(err.message || '添加节点失败')
  } finally {
    addNodeLoading.value = false
  }
}

async function handleInstallAgent(row) {
  try {
    await clusterApi.installAgent(row.id)
    ElMessage.success('Agent 安装任务已下发')
    await fetchNodes()
  } catch (err) {
    ElMessage.error(err.message || '安装 Agent 失败')
  }
}

async function handleCheckStatus(row) {
  try {
    const result = await clusterApi.checkStatus(row.id)
    ElMessage.success(`节点状态: ${result?.agentStatus || '未知'}`)
    await fetchNodes()
  } catch (err) {
    ElMessage.error(err.message || '检查状态失败')
  }
}

// ---- 初始化 ----
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.cluster-page {
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

.nodes-toolbar {
  margin-bottom: 12px;
}
</style>