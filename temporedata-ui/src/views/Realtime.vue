<template>
  <div class="realtime-page">
    <PageHeader title="实时计算" desc="管理 Flink 实时计算任务，支持 Kafka/CDC 数据源实时处理">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openAddDialog">新建任务</el-button>
      </template>
    </PageHeader>

    <div class="page-body">
      <el-table :data="pagedData" v-loading="loading" stripe>
        <el-table-column prop="name" label="任务名称" min-width="160" />
        <el-table-column prop="sourceType" label="数据源类型" width="120">
          <template #default="{ row }">
            <el-tag effect="dark">{{ row.sourceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :type="statusTagType(row.status)" :label="statusLabel(row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="lastStartTime" label="最近启动时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'RUNNING'" type="success" link :icon="VideoPlay" @click="handleStart(row)">启动</el-button>
            <el-button v-else type="warning" link :icon="VideoPause" @click="handleStop(row)">停止</el-button>
            <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该任务吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑任务' : '新建任务'" width="560px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入任务名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="数据源类型" prop="sourceType">
          <el-select v-model="form.sourceType" placeholder="请选择数据源类型" style="width: 100%">
            <el-option label="Kafka" value="KAFKA" />
            <el-option label="MySQL CDC" value="MYSQL_CDC" />
            <el-option label="PostgreSQL CDC" value="PGSQL_CDC" />
            <el-option label="Socket" value="SOCKET" />
          </el-select>
        </el-form-item>
        <el-form-item label="SQL 语句" prop="sql">
          <el-input v-model="form.sql" type="textarea" :rows="6" placeholder="请输入 Flink SQL 语句" />
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
import { Plus, Edit, Delete, VideoPlay, VideoPause } from '@element-plus/icons-vue'
import { realtimeApi } from '@/api/modules/realtime'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return tableData.value.slice(start, start + pageSize.value)
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({ name: '', sourceType: '', sql: '' })
const rules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  sql: [{ required: true, message: '请输入 SQL 语句', trigger: 'blur' }]
}

function statusTagType(status) {
  const map = { RUNNING: 'success', STOPPED: 'info', FAILED: 'danger' }
  return map[status] || 'default'
}
function statusLabel(status) {
  const map = { RUNNING: '运行中', STOPPED: '已停止', FAILED: '失败' }
  return map[status] || status
}

async function fetchData() {
  loading.value = true
  try {
    tableData.value = await realtimeApi.list()
  } catch (err) {
    ElMessage.error(err.message || '加载实时任务列表失败')
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, { name: '', sourceType: '', sql: '' })
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, { name: row.name, sourceType: row.sourceType, sql: row.sql || '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await realtimeApi.update(editId.value, { ...form })
      ElMessage.success('任务更新成功')
    } else {
      await realtimeApi.create({ ...form })
      ElMessage.success('任务创建成功')
    }
    dialogVisible.value = false
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleStart(row) {
  try {
    await realtimeApi.start(row.id)
    ElMessage.success('任务已启动')
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '启动失败')
  }
}

async function handleStop(row) {
  try {
    await realtimeApi.stop(row.id)
    ElMessage.success('任务已停止')
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '停止失败')
  }
}

async function handleDelete(id) {
  try {
    await realtimeApi.delete(id)
    ElMessage.success('任务已删除')
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.realtime-page { max-width: 1200px; }
.page-body {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>