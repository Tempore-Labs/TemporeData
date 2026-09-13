<template>
  <div class="page">
    <PageHeader title="黑白名单" subtitle="IP 访问控制 - 黑名单拦截 / 白名单放行">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新增条目</el-button>
      </template>
    </PageHeader>

    <div class="seg-bar">
      <el-radio-group v-model="filterType" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="BLACK">黑名单</el-radio-button>
        <el-radio-button value="WHITE">白名单</el-radio-button>
      </el-radio-group>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="ipAddress" label="IP 地址" min-width="160">
        <template #default="{ row }"><span class="mono">{{ row.ipAddress || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="row.listType === 'WHITE' ? 'success' : 'danger'">
            {{ row.listType === 'WHITE' ? '白名单' : '黑名单' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusBadge :status="row.status === 'INACTIVE' ? 'closed' : 'success'" :text="row.status === 'INACTIVE' ? '停用' : '生效'" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160">
        <template #default="{ row }"><span class="mono muted">{{ row.createTime || '—' }}</span></template>
      </el-table-column>
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

    <!-- create/edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑条目' : '新增条目'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="IP 地址" required>
          <el-input v-model="form.ipAddress" placeholder="如 10.0.0.5" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="form.listType">
            <el-radio value="BLACK">黑名单</el-radio>
            <el-radio value="WHITE">白名单</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="form.reason" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="生效" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
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
import { blacklistApi, type BlacklistEntity } from '@/api/blacklist'

const queryClient = useQueryClient()
const rows = ref<BlacklistEntity[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const filterType = ref('')

const dialogVisible = ref(false)
const editing = ref<BlacklistEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<BlacklistEntity>>({})

async function load() {
  loading.value = true
  try {
    const res = await blacklistApi.page({ page: page.value - 1, size })
    let list = res.content || []
    if (filterType.value) list = list.filter((b) => b.listType === filterType.value)
    rows.value = list
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
  form.value = { ipAddress: '', listType: 'BLACK', reason: '', status: 'ACTIVE' }
  dialogVisible.value = true
}
function openEdit(row: BlacklistEntity) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.ipAddress) {
    ElMessage.warning('IP 地址必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) await blacklistApi.update(form.value)
    else await blacklistApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['blacklist'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function remove(row: BlacklistEntity) {
  await ElMessageBox.confirm(`确认删除 ${row.ipAddress} ？`, '提示', { type: 'warning' })
  await blacklistApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.seg-bar {
  margin-bottom: 12px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.mono {
  font-family: var(--td-font-mono);
}
.muted {
  color: var(--td-text-4);
}
</style>