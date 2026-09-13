<template>
  <div class="page">
    <PageHeader title="权限审批" subtitle="资源访问权限申请与审批">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 申请资源</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" class="ma-tabs">
      <el-tab-pane label="全部申请" name="all">
        <el-skeleton v-if="loading" :rows="6" animated />
        <DataTable v-else :data="rows">
          <el-table-column prop="resourceName" label="资源名称" min-width="190">
            <template #default="{ row }"><span class="mono">{{ row.resourceName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="resourceType" label="类型" width="110">
            <template #default="{ row }">{{ row.resourceType || '—' }}</template>
          </el-table-column>
          <el-table-column prop="accessType" label="访问方式" width="120">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ row.accessType || '—' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="applicantName" label="申请人" width="110">
            <template #default="{ row }">{{ row.applicantName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button size="small" text type="success" @click="approve(row)">通过</el-button>
                <el-button size="small" text type="danger" @click="reject(row)">拒绝</el-button>
              </template>
              <el-button v-else size="small" text type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>

      <el-tab-pane label="待我审批" name="pending">
        <DataTable :data="pendingRows">
          <el-table-column prop="resourceName" label="资源名称" min-width="190">
            <template #default="{ row }"><span class="mono">{{ row.resourceName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="applicantName" label="申请人" width="120">
            <template #default="{ row }">{{ row.applicantName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="申请理由" min-width="260" show-overflow-tooltip />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="success" @click="approve(row)">通过</el-button>
              <el-button size="small" text type="danger" @click="reject(row)">拒绝</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>
    </el-tabs>
    <el-pagination
      v-if="tab === 'all' && total > size"
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="onPage"
    />

    <el-dialog v-model="dialogVisible" title="申请资源权限" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="资源名称" required>
          <el-input v-model="form.resourceName" />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="form.resourceType" style="width: 100%">
            <el-option v-for="s in ['TABLE', 'DATASET', 'API', 'FILE']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问方式">
          <el-select v-model="form.accessType" style="width: 100%">
            <el-option v-for="s in ['READ', 'WRITE', 'EXECUTE']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-input v-model="form.expireTime" placeholder="yyyy-MM-dd HH:mm:ss" />
        </el-form-item>
        <el-form-item label="申请理由">
          <el-input v-model="form.reason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { permapprovalApi, type PermapprovalEntity } from '@/api/ops.module'

const queryClient = useQueryClient()
const tab = ref('all')
const rows = ref<PermapprovalEntity[]>([])
const pendingRows = ref<PermapprovalEntity[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref<Partial<PermapprovalEntity>>({})

async function loadAll() {
  loading.value = true
  try {
    const res = await permapprovalApi.page({ page: page.value - 1, size })
    rows.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}
async function loadPending() {
  pendingRows.value = (await permapprovalApi.pending()) || []
}
async function load() {
  await Promise.all([loadAll(), loadPending()])
}
load()
watch(tab, (t) => {
  if (t === 'all') loadAll()
  else loadPending()
})

function onPage(p: number) {
  page.value = p
  loadAll()
}
function openCreate() {
  form.value = { resourceName: '', resourceType: 'TABLE', accessType: 'READ', status: 'PENDING' }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.resourceName) {
    ElMessage.warning('资源名称必填')
    return
  }
  saving.value = true
  try {
    await permapprovalApi.create(form.value)
    ElMessage.success('已提交申请')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['permapproval'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function approve(row: PermapprovalEntity) {
  await permapprovalApi.approve(row.id)
  ElMessage.success('已通过')
  await load()
}
async function reject(row: PermapprovalEntity) {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请', {
    confirmButtonText: '拒绝',
    cancelButtonText: '取消',
  })
  void value
  await permapprovalApi.reject(row.id)
  ElMessage.success('已拒绝')
  await load()
}
async function remove(row: PermapprovalEntity) {
  await ElMessageBox.confirm(`确认删除申请 ${row.resourceName} ？`, '提示', { type: 'warning' })
  await permapprovalApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page { padding: 24px; }
.ma-tabs { margin-top: 4px; }
.pager { margin-top: 16px; justify-content: flex-end; }
.mono { font-family: var(--td-font-mono); }
</style>