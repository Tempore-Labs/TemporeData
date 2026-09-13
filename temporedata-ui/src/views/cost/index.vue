<template>
  <div class="page">
    <PageHeader title="成本治理" subtitle="FinOps 成本台账与汇总">
      <template #actions>
        <el-button size="small" type="primary" @click="dialogVisible = true">+ 记录成本</el-button>
      </template>
    </PageHeader>

    <div class="cost-grid">
      <Card v-for="s in summaryCards" :key="s.label" class="cost-card">
        <div class="cost-card-label">{{ s.label }}</div>
        <div class="cost-card-value" :style="{ color: s.color }">{{ s.value }}</div>
        <div class="cost-card-sub">{{ s.sub }}</div>
      </Card>
    </div>

    <div class="query-bar">
      <span class="q-label">数据集ID</span>
      <el-input-number v-model="datasetId" :min="1" size="small" style="width: 140px" />
      <el-date-picker v-model="range" type="daterange" size="small" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" style="width: 260px" />
      <el-button size="small" type="primary" @click="loadSummary">汇总</el-button>
      <el-button size="small" @click="loadList">明细</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="recordDate" label="日期" width="130">
        <template #default="{ row }"><span class="mono">{{ row.recordDate }}</span></template>
      </el-table-column>
      <el-table-column prop="computeCost" label="计算成本" min-width="110">
        <template #default="{ row }"><span class="mono">¥{{ row.computeCost ?? 0 }}</span></template>
      </el-table-column>
      <el-table-column prop="storageCost" label="存储成本" min-width="110">
        <template #default="{ row }"><span class="mono">¥{{ row.storageCost ?? 0 }}</span></template>
      </el-table-column>
      <el-table-column prop="queryCost" label="查询成本" min-width="110">
        <template #default="{ row }"><span class="mono">¥{{ row.queryCost ?? 0 }}</span></template>
      </el-table-column>
      <el-table-column label="合计" min-width="110">
        <template #default="{ row }">
          <span class="mono total">{{ totalOf(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="记录时间" min-width="150">
        <template #default="{ row }"><span class="mono muted">{{ fmtTime(row.createdAt) }}</span></template>
      </el-table-column>
    </DataTable>

    <!-- Record dialog -->
    <el-dialog v-model="dialogVisible" title="记录成本" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="数据集ID" required>
          <el-input-number v-model="form.datasetId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计算成本">
          <el-input-number v-model="form.compute" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="存储成本">
          <el-input-number v-model="form.storage" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="查询成本">
          <el-input-number v-model="form.query" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="form.date" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import Card from '@/components/base/Card.vue'
import { costApi } from '@/api/cost'
import type { CostEntity, CostSummary } from '@/types/cost'

const queryClient = useQueryClient()
const rows = ref<CostEntity[]>([])
const loading = ref(false)
const summary = ref<CostSummary | null>(null)

const datasetId = ref(1)
const range = ref<[string, string] | null>(null)

const dialogVisible = ref(false)
const saving = ref(false)
const form = ref<{ datasetId: number; compute?: number; storage?: number; query?: number; date?: string }>({ datasetId: 1 })

const summaryCards = computed(() => {
  const s = summary.value
  return [
    { label: '计算成本', value: s ? `¥${s.computeCost ?? 0}` : '—', color: '#2563eb', sub: s ? `${s.dayCount} 天` : '选择数据汇总' },
    { label: '存储成本', value: s ? `¥${s.storageCost ?? 0}` : '—', color: '#10b981', sub: s ? `${s.dayCount} 天` : '选择数据汇总' },
    { label: '查询成本', value: s ? `¥${s.queryCost ?? 0}` : '—', color: '#f59e0b', sub: s ? `${s.dayCount} 天` : '选择数据汇总' },
    { label: '总成本', value: s ? `¥${(Number(s.computeCost) || 0) + (Number(s.storageCost) || 0) + (Number(s.queryCost) || 0)}` : '—', color: '#8b5cf6', sub: s ? `数据集 ${s.datasetId}` : '选择数据汇总' },
  ]
})

async function loadSummary() {
  if (!datasetId.value) {
    ElMessage.warning('请选择数据集 ID')
    return
  }
  if (!range.value?.[0] || !range.value?.[1]) {
    ElMessage.warning('请选择日期范围')
    return
  }
  loading.value = true
  try {
    summary.value = await costApi.summarize(datasetId.value, range.value[0], range.value[1])
    await loadList()
  } finally {
    loading.value = false
  }
}

async function loadList() {
  loading.value = true
  try {
    rows.value = await costApi.list(datasetId.value)
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await costApi.record(form.value.datasetId, form.value.compute, form.value.storage, form.value.query, form.value.date)
    ElMessage.success('成本已记录')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['cost'] })
    form.value = { datasetId: 1 }
  } finally {
    saving.value = false
  }
}

function totalOf(row: CostEntity) {
  return `¥${(Number(row.computeCost) || 0) + (Number(row.storageCost) || 0) + (Number(row.queryCost) || 0)}`
}
function fmtTime(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 19) : '—'
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.cost-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
.cost-card {
  padding: 16px;
}
.cost-card-label {
  font-size: 12px;
  color: var(--td-text-3);
}
.cost-card-value {
  font-size: 24px;
  font-weight: 700;
  margin: 6px 0 2px;
  font-family: var(--td-font-mono);
}
.cost-card-sub {
  font-size: 11px;
  color: var(--td-text-4);
}
.query-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.q-label {
  font-size: 12px;
  color: var(--td-text-3);
}
.mono {
  font-family: var(--td-font-mono);
}
.muted {
  color: var(--td-text-4);
}
.total {
  color: var(--td-primary);
  font-weight: 600;
}
</style>
