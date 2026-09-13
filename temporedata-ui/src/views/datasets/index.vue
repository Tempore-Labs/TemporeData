<template>
  <div class="page">
    <PageHeader title="数据集管理" subtitle="统一数据对象模型（Dataset）维护">
      <template #actions>
        <el-button size="small" :loading="crawling" @click="crawl">元数据采集</el-button>
        <el-button size="small" type="primary" @click="openCreate">+ 新建数据集</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />

    <DataTable v-else :data="rows">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="160">
        <template #default="{ row }"><span class="mono">{{ row.name }}</span></template>
      </el-table-column>
      <el-table-column prop="code" label="编码" min-width="140">
        <template #default="{ row }"><span class="mono">{{ row.code }}</span></template>
      </el-table-column>
      <el-table-column prop="layer" label="分层" width="90">
        <template #default="{ row }"><el-tag size="small">{{ row.layer || '—' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="securityLevel" label="安全级别" width="100">
        <template #default="{ row }">
          <StatusBadge :status="row.securityLevel ? 'pass' : 'pending'" :text="row.securityLevel || '—'" />
        </template>
      </el-table-column>
      <el-table-column prop="owner" label="Owner" width="120" />
      <el-table-column prop="updatedAt" label="更新时间" min-width="150">
        <template #default="{ row }"><span class="mono muted">{{ fmtTime(row.updatedAt) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="primary" @click="viewVersions(row)">版本</el-button>
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

    <!-- Create / edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑数据集' : '新建数据集'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="form.code" :disabled="!!editing" />
        </el-form-item>
        <el-form-item label="分层">
          <el-select v-model="form.layer" style="width: 100%">
            <el-option v-for="l in ['ods', 'dwd', 'ads', 'dim', 'other']" :key="l" :label="l.toUpperCase()" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="安全级别">
          <el-select v-model="form.securityLevel" style="width: 100%">
            <el-option label="L1 公开" value="L1" />
            <el-option label="L2 内部" value="L2" />
            <el-option label="L3 敏感" value="L3" />
            <el-option label="L4 机密" value="L4" />
          </el-select>
        </el-form-item>
        <el-form-item label="Owner">
          <el-input v-model="form.owner" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- Versions dialog -->
    <el-dialog v-model="versionVisible" title="元数据版本" width="560px">
      <DataTable v-if="versions.length" :data="versions">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="driftType" label="变更类型" width="110">
          <template #default="{ row }"><el-tag size="small" :type="driftTagType(row.driftType)">{{ row.driftType || 'NONE' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="采集时间" min-width="150">
          <template #default="{ row }"><span class="mono muted">{{ fmtTime(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="Schema" min-width="180">
          <template #default="{ row }">
            <el-tooltip :content="row.schemaJson" placement="top">
              <span class="mono schema-preview">{{ truncate(row.schemaJson) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
      </DataTable>
      <el-empty v-else description="暂无版本" :image-size="60" />
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
import { datasetApi } from '@/api/dataset'
import type { DatasetEntity, MetadataVersionEntity } from '@/types/dataset'

const queryClient = useQueryClient()
const rows = ref<DatasetEntity[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)

const dialogVisible = ref(false)
const editing = ref<DatasetEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<DatasetEntity>>({})

const versionVisible = ref(false)
const versions = ref<MetadataVersionEntity[]>([])
const crawling = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await datasetApi.page({ page: page.value - 1, size })
    rows.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}
load()
watch(page, load)

function onPage(p: number) {
  page.value = p
}

function openCreate() {
  editing.value = null
  form.value = { name: '', code: '', layer: 'ods', securityLevel: 'L2', owner: '' }
  dialogVisible.value = true
}
function openEdit(row: DatasetEntity) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.name || !form.value.code) {
    ElMessage.warning('名称与编码必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) await datasetApi.update(editing.value.id, form.value)
    else await datasetApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['datasets'] })
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row: DatasetEntity) {
  await ElMessageBox.confirm(`确认删除数据集 ${row.name} ？`, '提示', { type: 'warning' })
  await datasetApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}

async function viewVersions(row: DatasetEntity) {
  versions.value = await datasetApi.versions(row.id)
  versionVisible.value = true
}

async function crawl() {
  crawling.value = true
  try {
    const res = await datasetApi.crawl()
    ElMessage.success(`采集完成: ${JSON.stringify(res)?.slice(0, 120)}`)
    await load()
  } finally {
    crawling.value = false
  }
}

function fmtTime(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 19) : '—'
}
function truncate(s: string, n = 60) {
  return s.length > n ? s.slice(0, n) + '…' : s
}
function driftTagType(d?: string) {
  if (d === 'ADD') return 'success'
  if (d === 'DROP') return 'danger'
  if (d === 'TYPE_CHANGE' || d === 'COMBINED') return 'warning'
  return 'info'
}
</script>

<style scoped>
.page {
  padding: 24px;
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
.schema-preview {
  font-size: 11px;
  color: var(--td-text-3);
}
</style>
