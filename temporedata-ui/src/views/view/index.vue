<template>
  <div class="page">
    <PageHeader title="视图管理" subtitle="逻辑视图定义、发布与执行">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建视图</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="name" label="视图名称" min-width="200">
        <template #default="{ row }"><span class="mono">{{ row.name || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="130">
        <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="warning" :disabled="row.status === 'PUBLISHED'" @click="publish(row)">发布</el-button>
          <el-button size="small" text type="success" @click="execute(row)">执行</el-button>
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑视图' : '新建视图'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ['DRAFT', 'PUBLISHED', 'DISABLED']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
import StatusBadge from '@/components/base/StatusBadge.vue'
import { sysViewApi, type ViewEntity } from '@/api/ops2.module'

const rows = ref<ViewEntity[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<ViewEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<ViewEntity>>({})

async function load() {
  loading.value = true
  try {
    rows.value = (await sysViewApi.list()) || []
  } finally {
    loading.value = false
  }
}
onMounted(load)

function openCreate() {
  editing.value = null
  form.value = { name: '', status: 'DRAFT', description: '' }
  dialogVisible.value = true
}
function openEdit(row: ViewEntity) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.name) {
    ElMessage.warning('名称必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) await sysViewApi.update(editing.value.id, form.value)
    else await sysViewApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function publish(row: ViewEntity) {
  await sysViewApi.publish(row.id)
  ElMessage.success('已发布')
  await load()
}
async function execute(row: ViewEntity) {
  const res = await sysViewApi.execute(row.id)
  ElMessage.success(`执行结果: ${res}`)
}
async function remove(row: ViewEntity) {
  await ElMessageBox.confirm(`确认删除视图 ${row.name} ？`, '提示', { type: 'warning' })
  await sysViewApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page { padding: 24px; }
.mono { font-family: var(--td-font-mono); }
</style>