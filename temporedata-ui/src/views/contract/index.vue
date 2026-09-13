<template>
  <div class="page">
    <PageHeader title="数据契约" subtitle="数据集契约注册、激活与兼容性校验">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 注册契约</el-button>
        <el-button size="small" @click="checkVisible = true">兼容性校验</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="datasetId" label="数据集ID" width="100" />
      <el-table-column prop="compatibilityMode" label="兼容模式" width="130">
        <template #default="{ row }"><el-tag size="small" :type="modeTag(row.compatibilityMode)">{{ row.compatibilityMode || 'BACKWARD' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusBadge :status="row.status || 'pending'" />
        </template>
      </el-table-column>
      <el-table-column prop="owner" label="Owner" width="120" />
      <el-table-column prop="contractYaml" label="契约内容" min-width="260">
        <template #default="{ row }">
          <el-tooltip :content="row.contractYaml" placement="top">
            <span class="mono yaml-preview">{{ truncate(row.contractYaml) }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" min-width="150">
        <template #default="{ row }"><span class="mono muted">{{ fmtTime(row.updatedAt) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" :disabled="row.status === 'ACTIVE'" @click="activate(row)">激活</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <!-- Create dialog -->
    <el-dialog v-model="dialogVisible" title="注册数据契约" width="620px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="数据集ID" required>
          <el-input-number v-model="form.datasetId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="兼容模式">
          <el-select v-model="form.mode" style="width: 100%">
            <el-option label="BACKWARD（向后兼容）" value="BACKWARD" />
            <el-option label="FULL（完全一致）" value="FULL" />
          </el-select>
        </el-form-item>
        <el-form-item label="契约 YAML" required>
          <el-input v-model="form.yaml" type="textarea" :rows="8" class="mono-area"
            placeholder="dataset: ods_order_detail&#10;columns:&#10;  - name: order_id&#10;    type: BIGINT&#10;    nullable: false" />
        </el-form-item>
        <el-form-item label="Owner">
          <el-input v-model="form.owner" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">注册</el-button>
      </template>
    </el-dialog>

    <!-- Check dialog -->
    <el-dialog v-model="checkVisible" title="兼容性校验" width="560px">
      <el-form :model="checkForm" label-width="110px">
        <el-form-item label="数据集ID" required>
          <el-input-number v-model="checkForm.datasetId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="承诺列" required>
          <el-input v-model="checkForm.promised" placeholder="如 order_id,amount" />
        </el-form-item>
        <el-form-item label="实际提供列">
          <el-input v-model="checkForm.provided" placeholder="如 order_id,amount,status" />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="checkResult !== null"
        :type="checkResult ? 'success' : 'warning'"
        :title="checkResult ? '兼容：契约可满足' : '不兼容：存在差异'"
        :closable="false"
        style="margin-top: 12px"
      />
      <template #footer>
        <el-button @click="checkVisible = false">取消</el-button>
        <el-button type="primary" :loading="checking" @click="doCheck">校验</el-button>
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
import { contractApi } from '@/api/contract'
import type { ContractEntity } from '@/types/contract'

const queryClient = useQueryClient()
const rows = ref<ContractEntity[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({ datasetId: 1, mode: 'BACKWARD', yaml: '', owner: '' })

const checkVisible = ref(false)
const checking = ref(false)
const checkResult = ref<boolean | null>(null)
const checkForm = ref({ datasetId: 1, promised: '', provided: '' })

async function load() {
  loading.value = true
  try {
    rows.value = await contractApi.list()
  } finally {
    loading.value = false
  }
}
load()

function openCreate() {
  form.value = { datasetId: 1, mode: 'BACKWARD', yaml: '', owner: '' }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.yaml.trim()) {
    ElMessage.warning('契约 YAML 必填')
    return
  }
  saving.value = true
  try {
    await contractApi.create(form.value.datasetId, form.value.yaml, form.value.mode, form.value.owner)
    ElMessage.success('契约已注册')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['contract'] })
    await load()
  } finally {
    saving.value = false
  }
}

async function activate(row: ContractEntity) {
  await ElMessageBox.confirm(`确认激活数据集 ${row.datasetId} 的契约？`, '提示', { type: 'info' })
  await contractApi.activate(row.id)
  ElMessage.success('已激活')
  await load()
}

async function doCheck() {
  if (!checkForm.value.promised) {
    ElMessage.warning('承诺列必填')
    return
  }
  checking.value = true
  try {
    const res = await contractApi.check(checkForm.value.datasetId, checkForm.value.promised, checkForm.value.provided)
    checkResult.value = res.compatible
  } finally {
    checking.value = false
  }
}

function truncate(s: string, n = 90) {
  return s.length > n ? s.slice(0, n) + '…' : s
}
function fmtTime(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 19) : '—'
}
function modeTag(m?: string) {
  return m === 'FULL' ? 'success' : 'info'
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
.yaml-preview {
  font-size: 11px;
  color: var(--td-text-3);
  white-space: pre;
}
</style>
