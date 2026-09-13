<template>
  <div class="page">
    <PageHeader title="数据同步" subtitle="数据同步任务管理（创建/执行/删除）">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建同步</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="name" label="同步名称" min-width="180">
        <template #default="{ row }"><span class="mono">{{ row.name || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="源->目标" min-width="240">
        <template #default="{ row }">
          <span class="mono">{{ row.sourceTable || '?' }}</span>
          <el-icon style="margin: 0 6px; vertical-align: middle"><Right /></el-icon>
          <span class="mono">{{ row.targetTable || '?' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="syncMode" label="模式" width="110">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.syncMode || '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="batchSize" label="批大小" width="100" align="right">
        <template #default="{ row }"><span class="mono">{{ row.batchSize || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="success" @click="execute(row)">执行</el-button>
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑同步' : '新建同步'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="源表">
          <el-input v-model="form.sourceTable" />
        </el-form-item>
        <el-form-item label="目标表">
          <el-input v-model="form.targetTable" />
        </el-form-item>
        <el-form-item label="同步模式">
          <el-select v-model="form.syncMode" style="width: 100%">
            <el-option v-for="s in ['FULL', 'INCREMENTAL', 'CDC', 'MERGE']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="增量字段">
          <el-input v-model="form.incrementalColumn" />
        </el-form-item>
        <el-form-item label="批大小">
          <el-input v-model="form.batchSize" placeholder="如 1000" />
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
import { Right } from '@element-plus/icons-vue'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { syncApi, type SyncEntity } from '@/api/ops.module'

const queryClient = useQueryClient()
const rows = ref<SyncEntity[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<SyncEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<SyncEntity>>({})

async function load() {
  loading.value = true
  try {
    rows.value = (await syncApi.list()) || []
  } finally {
    loading.value = false
  }
}
load()

function openCreate() {
  editing.value = null
  form.value = { name: '', syncMode: 'FULL', status: 'READY' }
  dialogVisible.value = true
}
function openEdit(row: SyncEntity) {
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
    if (editing.value) await syncApi.update(editing.value.id, form.value)
    else await syncApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['sync'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function execute(row: SyncEntity) {
  await syncApi.execute(row.id)
  ElMessage.success('已触发同步')
  await load()
}
async function remove(row: SyncEntity) {
  await ElMessageBox.confirm(`确认删除同步 ${row.name} ？`, '提示', { type: 'warning' })
  await syncApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page { padding: 24px; }
.mono { font-family: var(--td-font-mono); }
</style>