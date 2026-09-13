<template>
  <div class="page">
    <PageHeader title="我的数据" subtitle="我获得的授权数据资源">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 添加授权</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="resourceName" label="资源名称" min-width="200">
        <template #default="{ row }"><span class="mono">{{ row.resourceName || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="resourceType" label="资源类型" width="120">
        <template #default="{ row }">{{ row.resourceType || '—' }}</template>
      </el-table-column>
      <el-table-column prop="accessType" label="访问方式" width="120">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.accessType || '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="expireTime" label="过期时间" min-width="140">
        <template #default="{ row }"><span class="mono">{{ row.expireTime || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="grantedBy" label="授权人" width="120">
        <template #default="{ row }">{{ row.grantedBy || '—' }}</template>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑授权' : '添加授权'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="资源名称" required>
          <el-input v-model="form.resourceName" />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="form.resourceType" style="width: 100%">
            <el-option v-for="s in ['TABLE', 'DATASET', 'API', 'FILE', 'REPORT']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问方式">
          <el-select v-model="form.accessType" style="width: 100%">
            <el-option v-for="s in ['READ', 'WRITE', 'EXECUTE', 'EXPORT']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-input v-model="form.expireTime" placeholder="yyyy-MM-dd HH:mm:ss" />
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
import { mydataApi, type MydataEntity } from '@/api/ops.module'

const queryClient = useQueryClient()
const rows = ref<MydataEntity[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const dialogVisible = ref(false)
const editing = ref<MydataEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<MydataEntity>>({})

async function load() {
  loading.value = true
  try {
    const res = await mydataApi.page({ page: page.value - 1, size })
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
  form.value = { resourceName: '', resourceType: 'TABLE', accessType: 'READ' }
  dialogVisible.value = true
}
function openEdit(row: MydataEntity) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.resourceName) {
    ElMessage.warning('资源名称必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) await mydataApi.update(form.value)
    else await mydataApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['mydata'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function remove(row: MydataEntity) {
  await ElMessageBox.confirm(`确认删除授权 ${row.resourceName} ？`, '提示', { type: 'warning' })
  await mydataApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page { padding: 24px; }
.pager { margin-top: 16px; justify-content: flex-end; }
.mono { font-family: var(--td-font-mono); }
</style>