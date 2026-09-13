<template>
  <div class="page">
    <PageHeader title="监控总览" subtitle="平台资源与任务监控记录">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建监控项</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="name" label="名称" min-width="180">
        <template #default="{ row }"><span class="mono">{{ row.name }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <StatusBadge :status="row.status || 'running'" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <!-- Create/edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑监控项' : '新建监控项'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ['RUNNING', 'PAUSED', 'STOPPED', 'ERROR']" :key="s" :label="s" :value="s" />
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
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { monitorApi, type MonitorEntity } from '@/api/monitor'

const queryClient = useQueryClient()
const rows = ref<MonitorEntity[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const editing = ref<MonitorEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<MonitorEntity>>({})

async function load() {
  loading.value = true
  try {
    rows.value = await monitorApi.list()
  } finally {
    loading.value = false
  }
}
load()

function openCreate() {
  editing.value = null
  form.value = { name: '', status: 'RUNNING', description: '' }
  dialogVisible.value = true
}
function openEdit(row: MonitorEntity) {
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
    if (editing.value) await monitorApi.update(form.value)
    else await monitorApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['monitor'] })
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row: MonitorEntity) {
  await ElMessageBox.confirm(`确认删除监控项 ${row.name} ？`, '提示', { type: 'warning' })
  await monitorApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.mono {
  font-family: var(--td-font-mono);
}
</style>
