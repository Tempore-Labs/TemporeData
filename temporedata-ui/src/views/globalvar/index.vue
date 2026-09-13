<template>
  <div class="page">
    <PageHeader title="全局变量" subtitle="数据开发全局变量配置">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建变量</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="name" label="变量名" min-width="200">
        <template #default="{ row }"><span class="mono">{{ row.name || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="130">
        <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="320" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>
    <el-pagination
      v-if="total > size"
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="onPage"
    />

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑变量' : '新建变量'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="变量名" required>
          <el-input v-model="form.name" placeholder="如 {project_name}" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ['ENABLED', 'DISABLED', 'ARCHIVED']" :key="s" :label="s" :value="s" />
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
import { globalvarApi, type GlobalvarEntity } from '@/api/ops.module'

const queryClient = useQueryClient()
const rows = ref<GlobalvarEntity[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const dialogVisible = ref(false)
const editing = ref<GlobalvarEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<GlobalvarEntity>>({})

async function load() {
  loading.value = true
  try {
    const res = await globalvarApi.page({ page: page.value - 1, size })
    rows.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}
load()
function onPage(p: number) {
  page.value = p
  load()
}
function openCreate() {
  editing.value = null
  form.value = { name: '', status: 'ENABLED', description: '' }
  dialogVisible.value = true
}
function openEdit(row: GlobalvarEntity) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.name) {
    ElMessage.warning('变量名必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) await globalvarApi.update(form.value)
    else await globalvarApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['globalvar'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function remove(row: GlobalvarEntity) {
  await ElMessageBox.confirm(`确认删除变量 ${row.name} ？`, '提示', { type: 'warning' })
  await globalvarApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page { padding: 24px; }
.pager { margin-top: 16px; justify-content: flex-end; }
.mono { font-family: var(--td-font-mono); }
</style>