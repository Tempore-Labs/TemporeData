<template>
  <div class="page">
    <PageHeader title="数据接入" subtitle="数据接入任务管理（创建/执行/删除）">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建接入</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="name" label="接入名称" min-width="180">
        <template #default="{ row }"><span class="mono">{{ row.name || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="120">
        <template #default="{ row }"><span class="mono">{{ row.type || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="datasourceName" label="数据源" min-width="140">
        <template #default="{ row }">{{ row.datasourceName || '—' }}</template>
      </el-table-column>
      <el-table-column prop="targetTable" label="目标表" min-width="140">
        <template #default="{ row }"><span class="mono">{{ row.targetTable || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="rowCount" label="行数" width="100" align="right">
        <template #default="{ row }"><span class="mono">{{ row.rowCount ?? '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
      </el-table-column>
      <el-table-column prop="lastRunTime" label="最近运行" min-width="140">
        <template #default="{ row }"><span class="mono">{{ row.lastRunTime || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="success" @click="execute(row)">执行</el-button>
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑接入' : '新建接入'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="s in ['JDBC', 'KAFKA', 'HDFS', 'API', 'FILE']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源ID">
          <el-input v-model="form.datasourceId" />
        </el-form-item>
        <el-form-item label="目标表">
          <el-input v-model="form.targetTable" />
        </el-form-item>
        <el-form-item label="配置">
          <el-input v-model="form.config" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ['READY', 'RUNNING', 'SUCCESS', 'FAILED', 'STOPPED']" :key="s" :label="s" :value="s" />
          </el-select>
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
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { ingestionApi, type IngestionEntity } from '@/api/ops.module'

const queryClient = useQueryClient()
const rows = ref<IngestionEntity[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<IngestionEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<IngestionEntity>>({})

async function load() {
  loading.value = true
  try {
    rows.value = (await ingestionApi.list()) || []
  } finally {
    loading.value = false
  }
}
load()

function openCreate() {
  editing.value = null
  form.value = { name: '', type: 'JDBC', status: 'READY' }
  dialogVisible.value = true
}
function openEdit(row: IngestionEntity) {
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
    if (editing.value) await ingestionApi.update(editing.value.id, form.value)
    else await ingestionApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['ingestion'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function execute(row: IngestionEntity) {
  await ingestionApi.execute(row.id)
  ElMessage.success('已触发执行')
  await load()
}
async function remove(row: IngestionEntity) {
  await ElMessageBox.confirm(`确认删除接入 ${row.name} ？`, '提示', { type: 'warning' })
  await ingestionApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page { padding: 24px; }
.mono { font-family: var(--td-font-mono); }
</style>