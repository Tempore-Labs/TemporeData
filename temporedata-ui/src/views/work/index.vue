<template>
  <div class="page">
    <PageHeader title="作业管理" subtitle="批量作业的创建、运行与运维">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建作业</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="name" label="作业名称" min-width="200">
        <template #default="{ row }"><span class="mono">{{ row.name || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="success" :disabled="row.status === 'RUNNING'" @click="run(row)">运行</el-button>
          <el-button size="small" text type="warning" :disabled="row.status !== 'RUNNING'" @click="stop(row)">停止</el-button>
          <el-button size="small" text type="primary" @click="copy(row)">复制</el-button>
          <el-button size="small" text type="info" @click="openEdit(row)">编辑</el-button>
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

    <!-- instances dialog -->
    <el-dialog v-model="instancesVisible" title="运行实例" width="720px">
      <DataTable :data="instances">
        <el-table-column v-for="k in instanceKeys" :key="k" :prop="k" :label="k" min-width="120">
          <template #default="{ row }"><span class="mono">{{ typeof row[k] === 'object' ? JSON.stringify(row[k]) : row[k] }}</span></template>
        </el-table-column>
      </DataTable>
      <template #footer>
        <el-button @click="instancesVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- create/edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑作业' : '新建作业'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ['READY', 'RUNNING', 'SUCCESS', 'FAILED', 'STOPPED']" :key="s" :label="s" :value="s" />
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
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { workApi, type WorkEntity } from '@/api/ops2.module'

const queryClient = useQueryClient()
const rows = ref<WorkEntity[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const dialogVisible = ref(false)
const editing = ref<WorkEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<WorkEntity>>({})
const instancesVisible = ref(false)
const instances = ref<Array<Record<string, unknown>>>([])
const instanceKeys = computed(() => (instances.value[0] ? Object.keys(instances.value[0]) : []))

async function load() {
  loading.value = true
  try {
    const res = await workApi.page({ page: page.value - 1, size })
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
  form.value = { name: '', status: 'READY', description: '' }
  dialogVisible.value = true
}
function openEdit(row: WorkEntity) {
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
    if (editing.value) await workApi.update(form.value)
    else await workApi.add(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['work'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function run(row: WorkEntity) {
  await workApi.run(row.id)
  ElMessage.success('已触发运行')
  await load()
}
async function stop(row: WorkEntity) {
  await workApi.stop(row.id)
  ElMessage.success('已停止')
  await load()
}
async function copy(row: WorkEntity) {
  await workApi.copy(row.id)
  ElMessage.success('已复制')
  await load()
}
async function openInstances(row: WorkEntity) {
  instances.value = (await workApi.instances(row.id)) || []
  instancesVisible.value = true
}
async function remove(row: WorkEntity) {
  await ElMessageBox.confirm(`确认删除作业 ${row.name} ？`, '提示', { type: 'warning' })
  await workApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}

// expose instances open for potential reuse
defineExpose({ openInstances })
</script>

<style scoped>
.page { padding: 24px; }
.pager { margin-top: 16px; justify-content: flex-end; }
.mono { font-family: var(--td-font-mono); }
</style>