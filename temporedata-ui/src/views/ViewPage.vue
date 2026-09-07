<template>
  <div class="view-page">
    <div class="page-header">
      <h2 class="page-title">视图管理</h2>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">新建视图</el-button>
    </div>

    <el-table :data="views" v-loading="loading" stripe>
      <el-table-column prop="name" label="视图名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="remark" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createDateTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <div class="action-btns">
            <el-button type="primary" link :icon="View" @click="handlePreview(row)">预览</el-button>
            <el-button v-if="row.status !== 'PUBLISHED'" type="success" link :icon="Upload" @click="handlePublish(row)">发布</el-button>
            <el-button type="info" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该视图吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑视图' : '新建视图'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="视图名称" prop="name">
          <el-input v-model="form.name" placeholder="视图名称" />
        </el-form-item>
        <el-form-item label="数据源" prop="datasourceId">
          <el-select v-model="form.datasourceId" placeholder="选择数据源" style="width: 100%">
            <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="remark">
          <el-input v-model="form.remark" placeholder="视图描述" />
        </el-form-item>
        <el-form-item label="SQL查询" prop="querySql">
          <el-input v-model="form.querySql" type="textarea" :rows="6" placeholder="SELECT * FROM table LIMIT 100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Preview Dialog -->
    <el-dialog v-model="previewVisible" title="数据预览" width="80%" fullscreen destroy-on-close>
      <template v-if="previewData">
        <div class="preview-meta">
          <span>总行数: <strong>{{ previewData.total }}</strong></span>
        </div>
        <el-table :data="previewRows" border stripe max-height="500">
          <el-table-column
            v-for="col in previewData.columns"
            :key="col.name"
            :prop="col.name"
            :label="col.name"
            min-width="120"
            show-overflow-tooltip
          />
        </el-table>
      </template>
      <el-empty v-else description="暂无数据" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, View, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { viewApi } from '@/api/modules/view'
import { datasourceApi } from '@/api/modules/datasource'

const loading = ref(false)
const views = ref([])
const datasources = ref([])

// Dialog
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const editingId = ref('')
const form = ref({ name: '', datasourceId: '', remark: '', querySql: '' })
const rules = {
  name: [{ required: true, message: '请输入视图名称', trigger: 'blur' }],
  datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  querySql: [{ required: true, message: '请输入SQL查询', trigger: 'blur' }]
}

// Preview
const previewVisible = ref(false)
const previewData = ref(null)
const previewRows = computed(() => {
  if (!previewData.value?.rows || !previewData.value?.columns) return []
  return previewData.value.rows.map(row => {
    const obj = {}
    previewData.value.columns.forEach((col, i) => { obj[col.name] = row[i] })
    return obj
  })
})

onMounted(() => {
  fetchViews()
  fetchDatasources()
})

async function fetchViews() {
  loading.value = true
  try { views.value = await viewApi.list() || [] } catch (e) { ElMessage.error('获取视图列表失败') } finally { loading.value = false }
}

async function fetchDatasources() {
  try { datasources.value = await datasourceApi.list() || [] } catch (e) { /* ignore */ }
}

function openAddDialog() {
  isEdit.value = false
  editingId.value = ''
  form.value = { name: '', datasourceId: '', remark: '', querySql: '' }
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editingId.value = row.id
  form.value = { name: row.name, datasourceId: row.datasourceId, remark: row.remark || '', querySql: row.querySql || '' }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (isEdit.value) {
      await viewApi.update(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await viewApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchViews()
  } catch (e) { ElMessage.error('操作失败') }
}

async function handleDelete(id) {
  try {
    await viewApi.delete(id)
    ElMessage.success('删除成功')
    fetchViews()
  } catch (e) { ElMessage.error('删除失败') }
}

async function handlePublish(row) {
  try {
    await viewApi.publish(row.id)
    ElMessage.success('发布成功')
    fetchViews()
  } catch (e) { ElMessage.error('发布失败') }
}

async function handlePreview(row) {
  previewVisible.value = true
  previewData.value = null
  try {
    previewData.value = await viewApi.execute(row.id)
  } catch (e) { ElMessage.error('查询失败') }
}
</script>

<style scoped>
.view-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.action-btns { display: flex; gap: 2px; flex-wrap: wrap; }
.preview-meta { margin-bottom: 12px; color: #6b7280; font-size: 14px; }
</style>