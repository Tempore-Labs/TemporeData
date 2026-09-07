<template>
  <div class="funcrepo-page">
    <div class="page-header">
      <h2 class="page-title">函数仓库</h2>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">新增函数</el-button>
    </div>

    <div class="page-body">
      <el-table :data="pagedData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="函数名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="funcTypeTag(row.type)" effect="light">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="language" label="语言" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.language }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
              <el-button type="warning" link :icon="VideoPlay" @click="handleTest(row)">测试</el-button>
              <el-popconfirm title="确定要删除该函数吗？" @confirm="handleDelete(row.id)">
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
      :title="isEdit ? '编辑函数' : '新增函数'"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" label-position="right">
        <el-form-item label="函数名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入函数名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择函数类型" style="width: 100%">
            <el-option label="UDF" value="UDF" />
            <el-option label="UDAF" value="UDAF" />
            <el-option label="UDTF" value="UDTF" />
          </el-select>
        </el-form-item>
        <el-form-item label="语言" prop="language">
          <el-select v-model="form.language" placeholder="请选择语言" style="width: 100%">
            <el-option label="Java" value="Java" />
            <el-option label="Scala" value="Scala" />
            <el-option label="Python" value="Python" />
          </el-select>
        </el-form-item>
        <el-form-item label="脚本内容" prop="script">
          <el-input
            v-model="form.script"
            type="textarea"
            :rows="8"
            placeholder="请输入函数脚本代码"
            class="code-editor"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入函数描述"
            maxlength="256"
          />
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
import { Plus, Edit, Delete, VideoPlay } from '@element-plus/icons-vue'
import { funcApi } from '@/api/modules/func'
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
  language: '',
  script: '',
  description: ''
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入函数名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择函数类型', trigger: 'change' }],
  language: [{ required: true, message: '请选择语言', trigger: 'change' }],
  script: [{ required: true, message: '请输入脚本内容', trigger: 'blur' }]
}

// ---- 工具函数 ----
function funcTypeTag(type) {
  const map = { UDF: '', UDAF: 'warning', UDTF: 'success' }
  return map[type] || 'info'
}

// ---- 加载数据 ----
async function fetchData() {
  loading.value = true
  try {
    tableData.value = await funcApi.list()
  } catch (err) {
    ElMessage.error(err.message || '加载函数列表失败')
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
    language: row.language,
    script: row.script || '',
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
      await funcApi.update(editId.value, payload)
      ElMessage.success('函数更新成功')
    } else {
      await funcApi.create(payload)
      ElMessage.success('函数创建成功')
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
    await funcApi.delete(id)
    ElMessage.success('函数已删除')
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
    await funcApi.test(row.id)
    ElMessage.success('函数测试已执行')
  } catch (err) {
    ElMessage.error(err.message || '函数测试失败')
  }
}

// ---- 初始化 ----
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.funcrepo-page {
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