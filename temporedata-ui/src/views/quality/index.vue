<template>
  <div class="page">
    <PageHeader title="质量管理" subtitle="质量规则与质量门禁">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建规则</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab">
      <el-tab-pane label="质量规则" name="rules">
        <el-skeleton v-if="loading" :rows="6" animated />
        <DataTable v-else :data="rows">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="datasetId" label="数据集ID" width="100" />
          <el-table-column prop="dimension" label="维度" width="120" />
          <el-table-column prop="ruleType" label="规则类型" width="140">
            <template #default="{ row }"><el-tag size="small">{{ row.ruleType || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="expression" label="表达式" min-width="220">
            <template #default="{ row }"><span class="mono">{{ row.expression || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="thresholdScore" label="阈值" width="100">
            <template #default="{ row }"><span class="mono">{{ row.thresholdScore ?? '—' }}</span></template>
          </el-table-column>
          <el-table-column label="拦截" width="90">
            <template #default="{ row }">
              <StatusBadge :status="row.isBlocking ? 'block' : 'pass'" :text="row.isBlocking ? '拦截' : '通过'" />
            </template>
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
      </el-tab-pane>

      <el-tab-pane label="质量门禁" name="gates">
        <el-skeleton v-if="gateLoading" :rows="4" animated />
        <template v-else>
          <div class="gate-list">
            <div v-for="g in gates" :key="g.id" class="gate-item">
              <div>
                <div class="gate-title">门禁 #{{ g.id }} · 数据集 {{ g.datasetId }}</div>
                <div class="gate-sub">规则 {{ g.ruleId ?? '—' }} · 最低分 {{ g.minScore ?? '—' }}</div>
              </div>
              <StatusBadge :status="g.status || 'closed'" />
            </div>
          </div>
          <div class="gate-actions">
            <el-button size="small" type="primary" @click="openGate">+ 新建门禁</el-button>
            <el-button size="small" @click="assessVisible = true">质量评估</el-button>
          </div>
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- Rule dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑规则' : '新建规则'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="数据集ID" required>
          <el-input-number v-model="form.datasetId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="维度">
          <el-select v-model="form.dimension" style="width: 100%">
            <el-option v-for="d in ['完整性', '唯一性', '一致性', '准确性', '及时性']" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则类型">
          <el-select v-model="form.ruleType" style="width: 100%">
            <el-option v-for="t in ['非空检查', '唯一性检查', '范围检查', '格式检查', '引用完整性']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="表达式">
          <el-input v-model="form.expression" type="textarea" :rows="3" placeholder="如 column = 'order_id' AND value IS NOT NULL" />
        </el-form-item>
        <el-form-item label="阈值分数">
          <el-input-number v-model="form.thresholdScore" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="失败拦截">
          <el-switch v-model="form.isBlocking" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- Gate dialog -->
    <el-dialog v-model="gateVisible" title="新建质量门禁" width="480px">
      <el-form :model="gateForm" label-width="90px">
        <el-form-item label="数据集ID" required>
          <el-input-number v-model="gateForm.datasetId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="规则ID">
          <el-input-number v-model="gateForm.ruleId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最低分数">
          <el-input-number v-model="gateForm.minScore" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="拦截">
          <el-switch v-model="gateForm.blocking" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gateVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveGate">保存</el-button>
      </template>
    </el-dialog>

    <!-- Assess dialog -->
    <el-dialog v-model="assessVisible" title="质量评估" width="420px">
      <el-form label-width="90px">
        <el-form-item label="数据集ID" required>
          <el-input-number v-model="assessForm.datasetId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="评分" required>
          <el-slider v-model="assessForm.score" :min="0" :max="100" show-input />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assessVisible = false">取消</el-button>
        <el-button type="primary" :loading="assessing" @click="doAssess">评估</el-button>
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
import { qualityApi } from '@/api/quality'
import type { QualityGateEntity, QualityRuleEntity } from '@/types/quality'

const queryClient = useQueryClient()
const tab = ref('rules')

const rows = ref<QualityRuleEntity[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)

const gates = ref<QualityGateEntity[]>([])
const gateLoading = ref(false)

const dialogVisible = ref(false)
const editing = ref<QualityRuleEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<QualityRuleEntity>>({})

const gateVisible = ref(false)
const gateForm = ref<Partial<QualityGateEntity>>({})

const assessVisible = ref(false)
const assessing = ref(false)
const assessForm = ref({ datasetId: 1, score: 90 })

async function load() {
  loading.value = true
  try {
    const res = await qualityApi.rulePage({ page: page.value - 1, size })
    rows.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}
load()
watch(page, load)

async function loadGates() {
  gateLoading.value = true
  try {
    gates.value = await qualityApi.gateList()
  } finally {
    gateLoading.value = false
  }
}
watch(tab, (t) => {
  if (t === 'gates') loadGates()
})

function onPage(p: number) {
  page.value = p
}

function openCreate() {
  editing.value = null
  form.value = { datasetId: 1, dimension: '完整性', ruleType: '非空检查', expression: '', thresholdScore: 90, isBlocking: false }
  dialogVisible.value = true
}
function openEdit(row: QualityRuleEntity) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.datasetId) {
    ElMessage.warning('数据集ID必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) await qualityApi.updateRule(editing.value.id, form.value)
    else await qualityApi.createRule(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['quality'] })
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row: QualityRuleEntity) {
  await ElMessageBox.confirm(`确认删除规则 #${row.id} ？`, '提示', { type: 'warning' })
  await qualityApi.deleteRule(row.id)
  ElMessage.success('已删除')
  await load()
}

async function saveGate() {
  if (!gateForm.value.datasetId) {
    ElMessage.warning('数据集ID必填')
    return
  }
  saving.value = true
  try {
    await qualityApi.createGate(gateForm.value)
    ElMessage.success('门禁已创建')
    gateVisible.value = false
    await loadGates()
  } finally {
    saving.value = false
  }
}

function openGate() {
  gateForm.value = { datasetId: 1, minScore: 90, blocking: false }
  gateVisible.value = true
}

async function doAssess() {
  assessing.value = true
  try {
    const res = await qualityApi.assess(assessForm.value.datasetId, assessForm.value.score)
    // Backend returns the QualityGateEntity (status PASSED/BLOCKED), not a score/passed object.
    const status = (res as any)?.status || '—'
    ElMessage.success(`评估完成：门禁状态 ${status}`)
    assessVisible.value = false
  } finally {
    assessing.value = false
  }
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
.gate-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.gate-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px solid var(--td-border);
  border-radius: 10px;
  background: var(--td-surface);
}
.gate-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--td-text-1);
}
.gate-sub {
  font-size: 11px;
  color: var(--td-text-4);
  margin-top: 2px;
}
.gate-actions {
  display: flex;
  gap: 8px;
}
</style>
