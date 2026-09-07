<template>
  <div class="datalevel-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>
        <el-icon style="margin-right: 8px;"><Grid /></el-icon>
        数据分级分类
      </h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        新增分级
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table :data="pagedData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="level" label="分级" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.level)" effect="dark" size="small">
              {{ levelLabel(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tableName" label="表名" min-width="160" show-overflow-tooltip />
        <el-table-column prop="columnName" label="列名" min-width="140" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openEditDialog(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-popconfirm title="确定删除该分级吗？" confirm-button-text="确定" cancel-button-text="取消" @confirm="handleDelete(row)">
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
      :title="isEdit ? '编辑分级' : '新增分级'"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" label-position="right">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分级名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="分级" prop="level">
          <el-select v-model="form.level" placeholder="请选择分级" style="width: 100%">
            <el-option label="L1 - 绝密" value="L1" />
            <el-option label="L2 - 机密" value="L2" />
            <el-option label="L3 - 内部" value="L3" />
            <el-option label="L4 - 公开" value="L4" />
          </el-select>
        </el-form-item>
        <el-form-item label="表名" prop="tableName">
          <el-input v-model="form.tableName" placeholder="请输入表名" />
        </el-form-item>
        <el-form-item label="列名" prop="columnName">
          <el-input v-model="form.columnName" placeholder="请输入列名" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
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
import { Plus, Edit, Delete, Grid } from '@element-plus/icons-vue'

// ---- 分级映射 ----
const levelMap = {
  L1: { label: 'L1 - 绝密', type: 'danger' },
  L2: { label: 'L2 - 机密', type: 'warning' },
  L3: { label: 'L3 - 内部', type: '' },
  L4: { label: 'L4 - 公开', type: 'success' }
}

function levelTagType(level) {
  return levelMap[level]?.type || 'info'
}

function levelLabel(level) {
  return levelMap[level]?.label || level
}

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
  level: '',
  tableName: '',
  columnName: '',
  description: ''
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入分级名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择分级', trigger: 'change' }],
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  columnName: [{ required: true, message: '请输入列名', trigger: 'blur' }]
}

// ---- 加载数据 ----
async function fetchData() {
  loading.value = true
  try {
    tableData.value = await securityApi.listLevels()
  } catch (err) {
    ElMessage.error(err.message || '加载分级列表失败')
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
    level: row.level,
    tableName: row.tableName,
    columnName: row.columnName,
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
      await securityApi.updateLevel(editId.value, payload)
      ElMessage.success('分级更新成功')
    } else {
      await securityApi.createLevel(payload)
      ElMessage.success('分级创建成功')
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
    await securityApi.deleteLevel(row.id)
    ElMessage.success('分级已删除')
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
.datalevel-page {
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
  display: flex;
  align-items: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>