<template>
  <div class="page">
    <PageHeader title="数据中心" subtitle="数据中心配置与资源统计">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建数据中心</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <template v-else>
      <!-- Top stats cards -->
      <div class="stat-grid" v-if="topRows.length">
        <el-card v-for="r in topRows" :key="r.id" shadow="never" class="dc-card">
          <div class="dc-title mono">{{ r.name }}</div>
          <div class="dc-category">{{ r.category || '未分类' }}</div>
          <div class="dc-stats">
            <div class="stat"><span class="num">{{ r.datasourceCount ?? 0 }}</span><span class="lbl">数据源</span></div>
            <div class="stat"><span class="num">{{ r.tableCount ?? 0 }}</span><span class="lbl">表</span></div>
            <div class="stat"><span class="num">{{ r.workflowCount ?? 0 }}</span><span class="lbl">工作流</span></div>
            <div class="stat"><span class="num">{{ r.apiCount ?? 0 }}</span><span class="lbl">API</span></div>
          </div>
        </el-card>
      </div>

      <DataTable :data="rows">
        <el-table-column prop="name" label="名称" min-width="180">
          <template #default="{ row }"><span class="mono">{{ row.name || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">{{ row.category || '—' }}</template>
        </el-table-column>
        <el-table-column prop="tableCount" label="表数" width="100" align="right">
          <template #default="{ row }"><span class="mono">{{ row.tableCount ?? '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="refreshInterval" label="刷新间隔(s)" width="130" align="right">
          <template #default="{ row }"><span class="mono">{{ row.refreshInterval ?? '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
        </el-table-column>
        <el-table-column column-key="desc" prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </DataTable>
    </template>
    <el-pagination
      v-if="total > size"
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="onPage"
    />

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑数据中心' : '新建数据中心'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" />
        </el-form-item>
        <el-form-item label="刷新间隔(s)">
          <el-input v-model.number="form.refreshInterval" />
        </el-form-item>
        <el-form-item label="数据源ID">
          <el-input v-model="form.datasourceId" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ['ACTIVE', 'INACTIVE', 'PAUSED']" :key="s" :label="s" :value="s" />
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
import { datacenterApi, type DatacenterEntity } from '@/api/ops.module'

const queryClient = useQueryClient()
const rows = ref<DatacenterEntity[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const dialogVisible = ref(false)
const editing = ref<DatacenterEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<DatacenterEntity>>({})

async function load() {
  loading.value = true
  try {
    const res = await datacenterApi.page({ page: page.value - 1, size })
    rows.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}
load()

const topRows = computed(() => rows.value.slice(0, 4))

function onPage(p: number) {
  page.value = p
  load()
}
function openCreate() {
  editing.value = null
  form.value = { name: '', category: '', refreshInterval: 60, status: 'ACTIVE' }
  dialogVisible.value = true
}
function openEdit(row: DatacenterEntity) {
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
    if (editing.value) await datacenterApi.update(form.value)
    else await datacenterApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['datacenter'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function remove(row: DatacenterEntity) {
  await ElMessageBox.confirm(`确认删除数据中心 ${row.name} ？`, '提示', { type: 'warning' })
  await datacenterApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page { padding: 24px; }
.stat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; margin-bottom: 20px; }
.dc-card { border-radius: 12px; }
.dc-title { font-size: 15px; font-weight: 600; }
.dc-category { color: var(--td-text-3); font-size: 12px; margin: 4px 0 12px; }
.dc-stats { display: flex; gap: 20px; }
.stat { display: flex; flex-direction: column; }
.stat .num { font-family: var(--td-font-mono); font-size: 20px; font-weight: 700; }
.stat .lbl { font-size: 12px; color: var(--td-text-3); }
.pager { margin-top: 16px; justify-content: flex-end; }
.mono { font-family: var(--td-font-mono); }
</style>