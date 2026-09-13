<template>
  <div class="page">
    <PageHeader title="数据报表" subtitle="报表创建、配置与发布管理">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建报表</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="name" label="报表名称" min-width="160">
        <template #default="{ row }"><span class="mono">{{ row.name }}</span></template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="130">
        <template #default="{ row }"><el-tag size="small" :type="typeTag(row.type)">{{ row.type || '—' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
      <el-table-column prop="datasourceId" label="数据源" min-width="120">
        <template #default="{ row }"><span class="mono">{{ row.datasourceId || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <StatusBadge :status="row.status" :text="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="150">
        <template #default="{ row }"><span class="mono muted">{{ row.createTime || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="primary" @click="togglePublish(row)">
            {{ row.status === 'PUBLISHED' ? '下架' : '发布' }}
          </el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <!-- create/edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑报表' : '新建报表'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="t in ['TABLE', 'CHART', 'PIVOT', 'BOARD', 'EXPORT']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源">
          <el-input v-model="form.datasourceId" />
        </el-form-item>
        <el-form-item label="配置">
          <el-input v-model="form.config" type="textarea" :rows="4" placeholder='{"chart":"bar","fields":["date","value"]}' />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
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
import { reportApi, type ReportEntity } from '@/api/report'

const queryClient = useQueryClient()
const rows = ref<ReportEntity[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const editing = ref<ReportEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<ReportEntity>>({})

async function load() {
  loading.value = true
  try {
    rows.value = await reportApi.list()
  } finally {
    loading.value = false
  }
}
load()

function openCreate() {
  editing.value = null
  form.value = { name: '', type: 'TABLE', datasourceId: '', config: '', description: '' }
  dialogVisible.value = true
}
function openEdit(row: ReportEntity) {
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
    if (editing.value) await reportApi.update(editing.value.id, form.value)
    else await reportApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['report'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function togglePublish(row: ReportEntity) {
  if (row.status === 'PUBLISHED') await reportApi.unpublish(row.id)
  else await reportApi.publish(row.id)
  ElMessage.success('操作成功')
  await load()
}
async function remove(row: ReportEntity) {
  await ElMessageBox.confirm(`确认删除报表 ${row.name} ？`, '提示', { type: 'warning' })
  await reportApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
function typeTag(t?: string) {
  if (t === 'BOARD') return 'warning'
  if (t === 'CHART') return 'success'
  if (t === 'EXPORT') return 'info'
  return 'primary'
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.mono {
  font-family: var(--td-font-mono);
}
.muted {
  color: var(--td-text-4);
}
</style>