<template>
  <div class="work-page">
    <div class="page-header">
      <h2 class="page-title">作业管理</h2>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">新建作业</el-button>
    </div>

    <!-- Toolbar -->
    <div class="toolbar">
      <el-select v-model="filterWorkflowId" placeholder="按作业流筛选" clearable style="width: 200px" @change="fetchList">
        <el-option v-for="wf in workflows" :key="wf.id" :label="wf.name" :value="wf.id" />
      </el-select>
      <el-input v-model="searchKey" placeholder="搜索作业名..." clearable style="width: 200px" />
      <el-button :icon="Search" @click="fetchList">查询</el-button>
    </div>

    <!-- Work List -->
    <el-table :data="pagedData" v-loading="loading" stripe>
      <el-table-column prop="name" label="作业名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="workType" label="类型" width="120">
        <template #default="{ row }">
          <el-tag :type="workTypeTag(row.workType)" effect="light">{{ row.workType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createDateTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <div class="action-btns">
            <el-button v-if="row.status !== 'RUNNING'" type="primary" link :icon="VideoPlay" @click="handleRun(row)">运行</el-button>
            <el-button v-else type="warning" link :icon="VideoPause" @click="handleStop(row)">停止</el-button>
            <el-button type="info" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="success" link :icon="CopyDocument" @click="handleCopy(row)">复制</el-button>
            <el-popconfirm title="确定删除该作业吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage" v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50]" :total="filteredData.length"
      layout="total, sizes, prev, pager, next" class="pagination"
    />

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑作业' : '新建作业'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="作业名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入作业名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="作业类型" prop="workType">
          <el-select v-model="form.workType" placeholder="请选择作业类型" style="width: 100%">
            <el-option label="Spark SQL" value="SPARK_SQL" />
            <el-option label="Flink SQL" value="FLINK_SQL" />
            <el-option label="Python" value="PYTHON" />
            <el-option label="Bash" value="BASH" />
            <el-option label="Curl" value="CURL" />
            <el-option label="API" value="API" />
            <el-option label="数据同步" value="SYNC_WORK" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属作业流" prop="workflowId">
          <el-select v-model="form.workflowId" placeholder="请选择作业流" clearable style="width: 100%">
            <el-option v-for="wf in workflows" :key="wf.id" :label="wf.name" :value="wf.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="配置" prop="config">
          <el-input v-model="form.config" type="textarea" :rows="5" placeholder="JSON格式配置" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Instance Log Dialog -->
    <el-dialog v-model="instanceVisible" title="执行实例" width="700px" destroy-on-close>
      <el-table :data="instances" stripe max-height="400">
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="execStartDateTime" label="开始时间" width="160" />
        <el-table-column prop="execEndDateTime" label="结束时间" width="160" />
        <el-table-column prop="duration" label="耗时(ms)" width="100" align="right" />
        <el-table-column prop="submitLog" label="日志" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, VideoPlay, VideoPause, CopyDocument, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { workApi } from '@/api/modules/work'
import { workflowApi } from '@/api/modules/workflow'

const loading = ref(false)
const works = ref([])
const workflows = ref([])
const filterWorkflowId = ref('')
const searchKey = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const filteredData = computed(() => {
  let data = works.value
  if (filterWorkflowId.value) data = data.filter(w => w.workflowId === filterWorkflowId.value)
  if (searchKey.value) {
    const kw = searchKey.value.toLowerCase()
    data = data.filter(w => w.name?.toLowerCase().includes(kw))
  }
  return data
})
const pagedData = computed(() => {
  const s = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(s, s + pageSize.value)
})

// Dialog
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ name: '', workType: 'SPARK_SQL', workflowId: '', remark: '', config: '' })
const rules = {
  name: [{ required: true, message: '请输入作业名称', trigger: 'blur' }],
  workType: [{ required: true, message: '请选择作业类型', trigger: 'change' }]
}
const editingId = ref('')

// Instance
const instanceVisible = ref(false)
const instances = ref([])

onMounted(() => {
  fetchWorkflows()
  fetchList()
})

async function fetchWorkflows() {
  try { workflows.value = await workflowApi.list() || [] } catch (e) { /* ignore */ }
}

async function fetchList() {
  loading.value = true
  try {
    if (filterWorkflowId.value) {
      works.value = await workApi.listByWorkflow(filterWorkflowId.value) || []
    } else {
      const page = await workApi.page(null, 0, 1000)
      works.value = page?.content || page || []
    }
  } catch (e) {
    ElMessage.error('获取作业列表失败')
  } finally { loading.value = false }
}

function openAddDialog() {
  isEdit.value = false
  editingId.value = ''
  form.value = { name: '', workType: 'SPARK_SQL', workflowId: filterWorkflowId.value || '', remark: '', config: '' }
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editingId.value = row.id
  form.value = { name: row.name, workType: row.workType, workflowId: row.workflowId || '', remark: row.remark || '', config: row.config || '' }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (isEdit.value) {
      await workApi.update(editingId.value, form.value.name, form.value.remark, form.value.config)
      ElMessage.success('更新成功')
    } else {
      await workApi.add(form.value.name, form.value.workType, form.value.workflowId || null)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function handleRun(row) {
  try {
    await workApi.run(row.id)
    ElMessage.success('作业已提交')
    fetchList()
  } catch (e) { ElMessage.error('运行失败') }
}

async function handleStop(row) {
  try {
    await workApi.stop(row.id)
    ElMessage.success('作业已停止')
    fetchList()
  } catch (e) { ElMessage.error('停止失败') }
}

async function handleCopy(row) {
  try {
    await workApi.copy(row.id)
    ElMessage.success('复制成功')
    fetchList()
  } catch (e) { ElMessage.error('复制失败') }
}

async function handleDelete(id) {
  try {
    await workApi.delete(id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (e) { ElMessage.error('删除失败') }
}

function workTypeTag(type) {
  const map = { SPARK_SQL: '', FLINK_SQL: 'success', PYTHON: 'warning', BASH: 'info', CURL: '', API: 'danger', SYNC_WORK: 'success' }
  return map[type] || 'info'
}

function statusTag(status) {
  const map = { DRAFT: 'info', PUBLISHED: 'success', RUNNING: 'warning', SUCCESS: 'success', FAIL: 'danger', STOPPED: 'info' }
  return map[status] || 'info'
}
</script>

<style scoped>
.work-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.toolbar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.action-btns { display: flex; gap: 2px; flex-wrap: wrap; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>