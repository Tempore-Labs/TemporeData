<template>
  <div class="engine-page">
    <div class="page-header">
      <h2 class="page-title">引擎作业</h2>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">新建作业</el-button>
    </div>

    <div class="page-body">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="Spark 作业" name="spark">
          <el-table :data="sparkPaged" v-loading="sparkLoading" stripe style="width: 100%">
            <el-table-column prop="name" label="作业名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="mainClass" label="主类" min-width="200" show-overflow-tooltip />
            <el-table-column prop="jarPath" label="JAR 路径" min-width="200" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <div class="action-btns">
                  <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
                  <el-popconfirm title="确定要删除该作业吗？" @confirm="handleDelete(row.id)">
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
              v-model:current-page="sparkPage"
              v-model:page-size="sparkPageSize"
              :page-sizes="[10, 20, 50]"
              :total="sparkTotal"
              layout="total, sizes, prev, pager, next"
              @size-change="sparkPage = 1"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="Flink 作业" name="flink">
          <el-table :data="flinkPaged" v-loading="flinkLoading" stripe style="width: 100%">
            <el-table-column prop="name" label="作业名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="mainClass" label="主类" min-width="200" show-overflow-tooltip />
            <el-table-column prop="jarPath" label="JAR 路径" min-width="200" show-overflow-tooltip />
            <el-table-column prop="parallelism" label="并行度" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <div class="action-btns">
                  <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
                  <el-popconfirm title="确定要删除该作业吗？" @confirm="handleDelete(row.id)">
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
              v-model:current-page="flinkPage"
              v-model:page-size="flinkPageSize"
              :page-sizes="[10, 20, 50]"
              :total="flinkTotal"
              layout="total, sizes, prev, pager, next"
              @size-change="flinkPage = 1"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- Add / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑作业' : '新建作业'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="dialogRules" label-width="100px">
        <el-form-item label="作业名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入作业名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="主类" prop="mainClass">
          <el-input v-model="form.mainClass" placeholder="请输入主类全限定名" maxlength="256" />
        </el-form-item>
        <el-form-item label="JAR 路径" prop="jarPath">
          <el-input v-model="form.jarPath" placeholder="请输入 JAR 文件路径" maxlength="512" />
        </el-form-item>
        <el-form-item v-if="activeTab === 'flink'" label="并行度" prop="parallelism">
          <el-input-number
            v-model="form.parallelism"
            :min="1"
            :max="128"
            placeholder="请输入并行度"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="参数" prop="args">
          <el-input
            v-model="form.args"
            type="textarea"
            :rows="3"
            placeholder="请输入作业参数，多个参数用空格分隔"
            maxlength="1024"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入作业描述"
            maxlength="256"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, Cpu } from '@element-plus/icons-vue'
import { engineApi } from '@/api/modules/engine'
import { ElMessage } from 'element-plus'

// ---- Tabs ----
const activeTab = ref('spark')

// ---- Spark Data ----
const sparkJobs = ref([])
const sparkLoading = ref(false)
const sparkPage = ref(1)
const sparkPageSize = ref(10)
const sparkTotal = computed(() => sparkJobs.value.length)

const sparkPaged = computed(() => {
  const start = (sparkPage.value - 1) * sparkPageSize.value
  return sparkJobs.value.slice(start, start + sparkPageSize.value)
})

// ---- Flink Data ----
const flinkJobs = ref([])
const flinkLoading = ref(false)
const flinkPage = ref(1)
const flinkPageSize = ref(10)
const flinkTotal = computed(() => flinkJobs.value.length)

const flinkPaged = computed(() => {
  const start = (flinkPage.value - 1) * flinkPageSize.value
  return flinkJobs.value.slice(start, start + flinkPageSize.value)
})

// ---- Dialog ----
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const editId = ref(null)

const form = reactive({
  name: '',
  mainClass: '',
  jarPath: '',
  parallelism: 1,
  args: '',
  description: ''
})

const commonRules = {
  name: [{ required: true, message: '请输入作业名称', trigger: 'blur' }],
  mainClass: [{ required: true, message: '请输入主类', trigger: 'blur' }],
  jarPath: [{ required: true, message: '请输入 JAR 路径', trigger: 'blur' }]
}

const flinkRules = {
  parallelism: [
    { required: true, message: '请输入并行度', trigger: 'blur' },
    { type: 'number', min: 1, message: '并行度至少为 1', trigger: 'blur' }
  ]
}

const dialogRules = computed(() => {
  if (activeTab.value === 'flink') {
    return { ...commonRules, ...flinkRules }
  }
  return commonRules
})

// ---- Status helpers ----
function statusTagType(status) {
  const map = { READY: 'info', RUNNING: '', SUCCESS: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { READY: '就绪', RUNNING: '运行中', SUCCESS: '成功', FAILED: '失败' }
  return map[status] || status
}

// ---- Fetch ----
async function fetchSparkJobs() {
  sparkLoading.value = true
  try {
    sparkJobs.value = await engineApi.listSpark()
  } catch (err) {
    ElMessage.error(err.message || '获取 Spark 作业列表失败')
  } finally {
    sparkLoading.value = false
  }
}

async function fetchFlinkJobs() {
  flinkLoading.value = true
  try {
    flinkJobs.value = await engineApi.listFlink()
  } catch (err) {
    ElMessage.error(err.message || '获取 Flink 作业列表失败')
  } finally {
    flinkLoading.value = false
  }
}

// ---- Tab change ----
function handleTabChange() {
  // reset pagination on tab switch handled by v-model per tab
}

// ---- CRUD ----
function resetForm() {
  form.name = ''
  form.mainClass = ''
  form.jarPath = ''
  form.parallelism = 1
  form.args = ''
  form.description = ''
  formRef.value?.resetFields()
}

function openAddDialog() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name || ''
  form.mainClass = row.mainClass || ''
  form.jarPath = row.jarPath || ''
  form.parallelism = row.parallelism || 1
  form.args = row.args || ''
  form.description = row.description || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      name: form.name,
      mainClass: form.mainClass,
      jarPath: form.jarPath,
      args: form.args,
      description: form.description
    }

    if (activeTab.value === 'flink') {
      payload.parallelism = form.parallelism
      if (isEdit.value) {
        await engineApi.updateFlink(editId.value, payload)
        ElMessage.success('Flink 作业更新成功')
      } else {
        await engineApi.createFlink(payload)
        ElMessage.success('Flink 作业创建成功')
      }
      await fetchFlinkJobs()
    } else {
      if (isEdit.value) {
        await engineApi.updateSpark(editId.value, payload)
        ElMessage.success('Spark 作业更新成功')
      } else {
        await engineApi.createSpark(payload)
        ElMessage.success('Spark 作业创建成功')
      }
      await fetchSparkJobs()
    }

    dialogVisible.value = false
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    if (activeTab.value === 'flink') {
      await engineApi.deleteFlink(id)
      ElMessage.success('Flink 作业已删除')
      await fetchFlinkJobs()
    } else {
      await engineApi.deleteSpark(id)
      ElMessage.success('Spark 作业已删除')
      await fetchSparkJobs()
    }
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

// ---- Init ----
onMounted(() => {
  fetchSparkJobs()
  fetchFlinkJobs()
})
</script>

<style scoped>
.engine-page {
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