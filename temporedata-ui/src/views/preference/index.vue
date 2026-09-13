<template>
  <div class="page">
    <PageHeader title="系统偏好" subtitle="用户偏好设置（键值对管理）">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新增偏好</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="prefKey" label="键" min-width="200">
        <template #default="{ row }"><span class="mono">{{ row.prefKey || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="prefValue" label="值" min-width="280" show-overflow-tooltip>
        <template #default="{ row }"><span class="mono">{{ row.prefValue || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="userId" label="用户" width="130">
        <template #default="{ row }">{{ row.userId || '全局' }}</template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" min-width="150">
        <template #default="{ row }"><span class="mono">{{ row.updateTime || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑偏好' : '新增偏好'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="键" required>
          <el-input v-model="form.prefKey" placeholder="如 theme / lang / page_size" />
        </el-form-item>
        <el-form-item label="值">
          <el-input v-model="form.prefValue" />
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="form.userId" placeholder="留空为全局" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import { preferenceApi, type PreferenceEntity } from '@/api/ops2.module'

const rows = ref<PreferenceEntity[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<PreferenceEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<PreferenceEntity>>({})

async function load() {
  loading.value = true
  try {
    rows.value = (await preferenceApi.all()) || []
  } finally {
    loading.value = false
  }
}
onMounted(load)

function openCreate() {
  editing.value = null
  form.value = { prefKey: '', prefValue: '', userId: '' }
  dialogVisible.value = true
}
function openEdit(row: PreferenceEntity) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.prefKey) {
    ElMessage.warning('键必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) await preferenceApi.update(form.value)
    else await preferenceApi.save(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function remove(row: PreferenceEntity) {
  await ElMessageBox.confirm(`确认删除偏好 ${row.prefKey} ？`, '提示', { type: 'warning' })
  await preferenceApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page { padding: 24px; }
.mono { font-family: var(--td-font-mono); }
</style>