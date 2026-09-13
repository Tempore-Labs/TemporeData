<template>
  <div class="page">
    <PageHeader title="动态表单" subtitle="数据服务动态表单配置与提交">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建表单</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="name" label="表单名称" min-width="180">
        <template #default="{ row }"><span class="mono">{{ row.name || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="formId" label="Form ID" min-width="140">
        <template #default="{ row }"><span class="mono">{{ row.formId || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
      </el-table-column>
      <el-table-column prop="shareToken" label="分享Token" min-width="140">
        <template #default="{ row }"><span class="mono">{{ row.shareToken || '—' }}</span></template>
      </el-table-column>
      <el-table-column column-key="desc" prop="description" label="描述" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="makeShare(row)">分享</el-button>
          <el-button size="small" text type="warning" @click="viewSubs(row)">提交记录</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑表单' : '新建表单'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="表单ID">
          <el-input v-model="form.formId" />
        </el-form-item>
        <el-form-item label="配置">
          <el-input v-model="form.config" type="textarea" :rows="4" placeholder="JSON schema" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ['ACTIVE', 'DRAFT', 'CLOSED']" :key="s" :label="s" :value="s" />
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

    <el-dialog v-model="subsVisible" title="提交记录" width="640px">
      <DataTable :data="submissions">
        <el-table-column label="提交数据" min-width="400">
          <template #default="{ row }"><pre class="mono subs-pre">{{ JSON.stringify(row, null, 2) }}</pre></template>
        </el-table-column>
      </DataTable>
      <template #footer>
        <el-button @click="subsVisible = false">关闭</el-button>
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
import { formApi, type FormEntity } from '@/api/ops.module'

const queryClient = useQueryClient()
const rows = ref<FormEntity[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<FormEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<FormEntity>>({})
const subsVisible = ref(false)
const submissions = ref<Array<Record<string, unknown>>>([])

async function load() {
  loading.value = true
  try {
    rows.value = (await formApi.list()) || []
  } finally {
    loading.value = false
  }
}
load()

function openCreate() {
  editing.value = null
  form.value = { name: '', status: 'DRAFT' }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.name) {
    ElMessage.warning('名称必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) await formApi.update(editing.value.id, form.value)
    else await formApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['form'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function makeShare(row: FormEntity) {
  const token = await formApi.shareToken(row.id)
  ElMessage.success(`分享Token: ${token}`)
  await load()
}
async function viewSubs(row: FormEntity) {
  submissions.value = (await formApi.submissions(row.id)) || []
  subsVisible.value = true
}
async function remove(row: FormEntity) {
  await ElMessageBox.confirm(`确认删除表单 ${row.name} ？`, '提示', { type: 'warning' })
  await formApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page { padding: 24px; }
.mono { font-family: var(--td-font-mono); }
.subs-pre { margin: 0; font-size: 12px; max-height: 300px; overflow: auto; white-space: pre-wrap; word-break: break-all; }
</style>