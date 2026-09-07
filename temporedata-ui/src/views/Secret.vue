<template>
  <div class="secret-page">
    <PageHeader title="密钥管理" desc="管理 API 密钥与连接凭证，支持作用域与描述管理">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openAddDialog">新增密钥</el-button>
      </template>
    </PageHeader>

    <div class="page-body">
      <el-table :data="pagedData" v-loading="loading" stripe>
        <el-table-column prop="key" label="密钥名称" min-width="160" />
        <el-table-column label="密钥值" min-width="240">
          <template #default="{ row }">
            <code class="secret-key">{{ maskSecret(row.value) }}</code>
            <el-button type="primary" link size="small" @click="handleCopy(row)">复制</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
        <el-table-column label="作用域" width="120">
          <template #default="{ row }">
            <el-tag :effect="row.scope === 'GLOBAL' ? 'dark' : 'light'">{{ row.scope || 'GLOBAL' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该密钥吗？" @confirm="handleDelete(row.id)">
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑密钥' : '新增密钥'" width="520px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="密钥名称" prop="key">
          <el-input v-model="form.key" placeholder="请输入密钥名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="密钥值" prop="value">
          <el-input v-model="form.value" type="textarea" :rows="2" placeholder="请输入密钥值" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" placeholder="请输入描述" maxlength="200" />
        </el-form-item>
        <el-form-item label="作用域" prop="scope">
          <el-select v-model="form.scope" placeholder="请选择作用域" style="width: 100%">
            <el-option label="全局 GLOBAL" value="GLOBAL" />
            <el-option label="工作流 WORKFLOW" value="WORKFLOW" />
          </el-select>
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
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { secretApi } from '@/api/modules/secret'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'

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
const form = reactive({ key: '', value: '', description: '', scope: 'GLOBAL' })
const rules = {
  key: [{ required: true, message: '请输入密钥名称', trigger: 'blur' }],
  value: [{ required: true, message: '请输入密钥值', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    tableData.value = await secretApi.list()
  } catch (err) {
    ElMessage.error(err.message || '加载密钥列表失败')
  } finally {
    loading.value = false
  }
}

function maskSecret(value) {
  if (!value) return ''
  if (value.length <= 8) return '********'
  return value.slice(0, 4) + '****' + value.slice(-4)
}

function openAddDialog() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, { key: '', value: '', description: '', scope: 'GLOBAL' })
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, { key: row.key, value: row.value || '', description: row.description || '', scope: row.scope || 'GLOBAL' })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await secretApi.update(editId.value, { ...form })
      ElMessage.success('密钥更新成功')
    } else {
      await secretApi.create({ ...form })
      ElMessage.success('密钥新增成功')
    }
    dialogVisible.value = false
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(id) {
  try {
    await secretApi.delete(id)
    ElMessage.success('密钥已删除')
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

function handleCopy(row) {
  navigator.clipboard.writeText(row.value).then(() => {
    ElMessage.success('密钥已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

onMounted(() => fetchData())
</script>

<style scoped>
.secret-page { max-width: 1200px; }
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
.secret-key {
  font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
  color: #6b7280;
  margin-right: 8px;
}
</style>