<template>
  <div class="page">
    <PageHeader title="审批中心" subtitle="通用业务审批流程管理">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 发起审批</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" class="ap-tabs">
      <el-tab-pane label="全部申请" name="all">
        <el-skeleton v-if="loading" :rows="6" animated />
        <DataTable v-else :data="rows">
          <el-table-column prop="title" label="标题" min-width="220">
            <template #default="{ row }"><span class="mono">{{ row.title || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="workflowId" label="流程" width="140">
            <template #default="{ row }"><span class="mono">{{ row.workflowId || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="applicantId" label="申请人" width="120">
            <template #default="{ row }">{{ row.applicantId || '—' }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" min-width="150">
            <template #default="{ row }"><span class="mono">{{ row.createTime || '—' }}</span></template>
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
          <el-table-column prop="title" label="标题" min-width="220">
            <template #default="{ row }"><span class="mono">{{ row.title || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="applicantId" label="申请人" width="130">
            <template #default="{ row }">{{ row.applicantId || '—' }}</template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" title="发起审批" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="流程ID">
          <el-input v-model="form.workflowId" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
import { approvalApi, type ApprovalEntity } from '@/api/ops2.module'

const queryClient = useQueryClient()
const tab = ref('all')
const rows = ref<ApprovalEntity[]>([])
const pendingRows = ref<ApprovalEntity[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref<Partial<ApprovalEntity>>({})

async function loadAll() {
  loading.value = true
  try {
    const res = await approvalApi.page({ page: page.value - 1, size })
    rows.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}
async function loadPending() {
  pendingRows.value = (await approvalApi.pending()) || []
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
  form.value = { title: '', workflowId: '', status: 'PENDING' }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.title) {
    ElMessage.warning('标题必填')
    return
  }
  saving.value = true
  try {
    await approvalApi.create(form.value)
    ElMessage.success('已提交')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['approval'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function approve(row: ApprovalEntity) {
  await approvalApi.approve(row.id)
  ElMessage.success('已通过')
  await load()
}
async function reject(row: ApprovalEntity) {
  await ElMessageBox.confirm(`确认拒绝审批「${row.title}」？`, '提示', { type: 'warning' })
  await approvalApi.reject(row.id)
  ElMessage.success('已拒绝')
  await load()
}
async function remove(row: ApprovalEntity) {
  await ElMessageBox.confirm(`确认删除审批「${row.title}」？`, '提示', { type: 'warning' })
  await approvalApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page { padding: 24px; }
.ap-tabs { margin-top: 4px; }
.pager { margin-top: 16px; justify-content: flex-end; }
.mono { font-family: var(--td-font-mono); }
</style>