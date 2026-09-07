<template>
  <div class="datamask-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>数据脱敏</h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        新增脱敏规则
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table :data="pagedData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="规则名称" min-width="140" />
        <el-table-column label="脱敏类型" width="130">
          <template #default="{ row }">
            <el-tag :type="maskTypeTag(row.maskType)" effect="light">{{ maskTypeLabel(row.maskType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tableName" label="表名" min-width="140" />
        <el-table-column prop="columnName" label="列名" min-width="140" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="dark">
              {{ row.status === 'ENABLED' ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="脱敏预览" min-width="160">
          <template #default="{ row }">
            <span class="mask-preview">{{ maskPreview(row.maskType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openEditDialog(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-popconfirm title="确定删除该脱敏规则吗？" confirm-button-text="确定" cancel-button-text="取消" @confirm="handleDelete(row)">
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

    <!-- 脱敏预览说明 -->
    <el-card shadow="never" class="preview-card">
      <template #header>
        <div class="preview-card-header">
          <el-icon><View /></el-icon>
          <span>脱敏预览示例</span>
        </div>
      </template>
      <div class="preview-list">
        <div class="preview-item">
          <el-tag type="warning" effect="light">手机号</el-tag>
          <span class="preview-arrow">→</span>
          <code>138****1234</code>
        </div>
        <div class="preview-item">
          <el-tag effect="light">邮箱</el-tag>
          <span class="preview-arrow">→</span>
          <code>u***@example.com</code>
        </div>
        <div class="preview-item">
          <el-tag type="danger" effect="light">身份证</el-tag>
          <span class="preview-arrow">→</span>
          <code>320***********1234</code>
        </div>
        <div class="preview-item">
          <el-tag type="success" effect="light">姓名</el-tag>
          <span class="preview-arrow">→</span>
          <code>张*</code>
        </div>
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑脱敏规则' : '新增脱敏规则'"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="right">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入规则名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="脱敏类型" prop="maskType">
          <el-select v-model="form.maskType" placeholder="请选择脱敏类型" style="width: 100%" @change="onMaskTypeChange">
            <el-option label="手机号" value="PHONE" />
            <el-option label="邮箱" value="EMAIL" />
            <el-option label="身份证" value="ID_CARD" />
            <el-option label="姓名" value="NAME" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="表名" prop="tableName">
          <el-input v-model="form.tableName" placeholder="请输入表名" />
        </el-form-item>
        <el-form-item label="列名" prop="columnName">
          <el-input v-model="form.columnName" placeholder="请输入列名" />
        </el-form-item>
        <el-form-item v-if="form.maskType === 'CUSTOM'" label="脱敏模式" prop="maskPattern">
          <el-input v-model="form.maskPattern" placeholder="请输入自定义脱敏正则模式" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" maxlength="200" show-word-limit />
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
import { securityApi } from '@/api/modules/security'
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
  maskType: '',
  tableName: '',
  columnName: '',
  maskPattern: '',
  description: ''
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  maskType: [{ required: true, message: '请选择脱敏类型', trigger: 'change' }],
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  columnName: [{ required: true, message: '请输入列名', trigger: 'blur' }],
  maskPattern: [{ required: true, message: '请输入脱敏模式', trigger: 'blur' }]
}

// ---- 脱敏类型标签 ----
const maskTypeMap = {
  PHONE: { label: '手机号', type: 'warning' },
  EMAIL: { label: '邮箱', type: '' },
  ID_CARD: { label: '身份证', type: 'danger' },
  NAME: { label: '姓名', type: 'success' },
  CUSTOM: { label: '自定义', type: 'info' }
}

function maskTypeLabel(type) {
  return maskTypeMap[type]?.label || type
}

function maskTypeTag(type) {
  return maskTypeMap[type]?.type || 'info'
}

// ---- 脱敏预览 ----
const previewMap = {
  PHONE: '138****1234',
  EMAIL: 'u***@example.com',
  ID_CARD: '320***********1234',
  NAME: '张*'
}

function maskPreview(type) {
  return previewMap[type] || '-'
}

// ---- 切换类型时清空自定义模式 ----
function onMaskTypeChange() {
  if (form.maskType !== 'CUSTOM') {
    form.maskPattern = ''
  }
}

// ---- 加载数据 ----
async function fetchData() {
  loading.value = true
  try {
    tableData.value = await securityApi.listMasks()
  } catch (err) {
    ElMessage.error(err.message || '加载脱敏规则列表失败')
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
    name: row.name || '',
    maskType: row.maskType || '',
    tableName: row.tableName || '',
    columnName: row.columnName || '',
    maskPattern: row.maskPattern || '',
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
    const payload = {
      name: form.name,
      maskType: form.maskType,
      tableName: form.tableName,
      columnName: form.columnName,
      maskPattern: form.maskPattern,
      description: form.description
    }
    if (isEdit.value) {
      await securityApi.updateMask(editId.value, payload)
      ElMessage.success('脱敏规则更新成功')
    } else {
      await securityApi.createMask(payload)
      ElMessage.success('脱敏规则创建成功')
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
async function handleDelete(row) {
  try {
    await securityApi.deleteMask(row.id)
    ElMessage.success('脱敏规则已删除')
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
.datamask-page {
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

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.mask-preview {
  font-family: 'Courier New', Courier, monospace;
  color: #6b7280;
  font-size: 13px;
}

.preview-card {
  margin-top: 16px;
}

.preview-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
}

.preview-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-arrow {
  color: #9ca3af;
  font-size: 14px;
}

.preview-item code {
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 13px;
  color: #374151;
}
</style>