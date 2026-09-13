<template>
  <div class="page">
    <PageHeader title="资产指标" subtitle="资产目录与业务指标管理">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreateMetric">+ 新建指标</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab">
      <el-tab-pane label="资产目录" name="catalog">
        <div class="filter-bar">
          <el-input v-model="tagKw" placeholder="按标签搜索..." size="small" style="width: 220px" clearable @keyup.enter="loadCatalog" />
          <el-button size="small" @click="loadCatalog">搜索</el-button>
        </div>
        <el-skeleton v-if="catalogLoading" :rows="6" animated />
        <DataTable v-else :data="catalogRows">
          <el-table-column prop="datasetId" label="数据集ID" width="100" />
          <el-table-column prop="businessTerm" label="业务术语" min-width="160" />
          <el-table-column prop="tags" label="标签" min-width="200">
            <template #default="{ row }">
              <el-tag v-for="t in (row.tags || '').split(',').filter(Boolean)" :key="t" size="small" class="mr-tag">{{ t }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="popularity" label="热度" width="100">
            <template #default="{ row }"><span class="mono">{{ row.popularity ?? '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="owner" label="Owner" width="120" />
        </DataTable>
        <el-pagination
          v-if="catalogTotal > 10"
          class="pager"
          layout="total, prev, pager, next"
          :total="catalogTotal"
          :page-size="10"
          :current-page="catalogPage"
          @current-change="(p: number) => { catalogPage = p; loadCatalog() }"
        />
      </el-tab-pane>

      <el-tab-pane label="业务指标" name="metric">
        <el-skeleton v-if="metricLoading" :rows="6" animated />
        <DataTable v-else :data="metricRows">
          <el-table-column prop="code" label="编码" width="130">
            <template #default="{ row }"><span class="mono">{{ row.code }}</span></template>
          </el-table-column>
          <el-table-column prop="name" label="名称" min-width="150" />
          <el-table-column prop="metricType" label="类型" width="110">
            <template #default="{ row }"><el-tag size="small">{{ row.metricType || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="datasetId" label="数据集ID" width="100" />
          <el-table-column prop="owner" label="Owner" width="120" />
          <el-table-column prop="definitionSql" label="定义 SQL" min-width="220">
            <template #default="{ row }">
              <el-tooltip :content="row.definitionSql || ''" placement="top">
                <span class="mono sql-preview">{{ row.definitionSql || '—' }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openEditMetric(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="removeMetric(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
        <el-pagination
          v-if="metricTotal > 10"
          class="pager"
          layout="total, prev, pager, next"
          :total="metricTotal"
          :page-size="10"
          :current-page="metricPage"
          @current-change="(p: number) => { metricPage = p; loadMetric() }"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- Metric dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑指标' : '新建指标'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="编码" required>
          <el-input v-model="form.code" :disabled="!!editing" placeholder="如 daily_gmv" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.metricType" style="width: 100%">
            <el-option v-for="t in ['SUM', 'COUNT', 'AVG', 'RATIO', 'CUSTOM']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据集ID">
          <el-input-number v-model="form.datasetId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="定义 SQL">
          <el-input v-model="form.definitionSql" type="textarea" :rows="4" />
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import { assetApi } from '@/api/asset'
import type { AssetCatalogEntity, MetricEntity } from '@/types/asset'

const queryClient = useQueryClient()
const tab = ref('catalog')

const tagKw = ref('')
const catalogRows = ref<AssetCatalogEntity[]>([])
const catalogLoading = ref(false)
const catalogTotal = ref(0)
const catalogPage = ref(1)

const metricRows = ref<MetricEntity[]>([])
const metricLoading = ref(false)
const metricTotal = ref(0)
const metricPage = ref(1)

const dialogVisible = ref(false)
const editing = ref<MetricEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<MetricEntity>>({})

async function loadCatalog() {
  catalogLoading.value = true
  try {
    if (tagKw.value) {
      catalogRows.value = await assetApi.catalogSearch(tagKw.value)
      catalogTotal.value = catalogRows.value.length
    } else {
      const res = await assetApi.catalogPage({ page: catalogPage.value - 1, size: 10 })
      catalogRows.value = res.content || []
      catalogTotal.value = res.totalElements || 0
    }
  } finally {
    catalogLoading.value = false
  }
}

async function loadMetric() {
  metricLoading.value = true
  try {
    const res = await assetApi.metricPage({ page: metricPage.value - 1, size: 10 })
    metricRows.value = res.content || []
    metricTotal.value = res.totalElements || 0
  } finally {
    metricLoading.value = false
  }
}

onMounted(loadCatalog)
watch(tab, (t) => {
  if (t === 'catalog') loadCatalog()
  else loadMetric()
})

function openCreateMetric() {
  editing.value = null
  form.value = { code: '', name: '', metricType: 'SUM', datasetId: 1, definitionSql: '', owner: '' }
  dialogVisible.value = true
}
function openEditMetric(row: MetricEntity) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.code || !form.value.name) {
    ElMessage.warning('编码与名称必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) await assetApi.metricUpdate(editing.value.id, form.value)
    else await assetApi.metricCreate(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['asset'] })
    await loadMetric()
  } finally {
    saving.value = false
  }
}

async function removeMetric(row: MetricEntity) {
  await ElMessageBox.confirm(`确认删除指标 ${row.name} ？`, '提示', { type: 'warning' })
  await assetApi.metricDelete(row.id)
  ElMessage.success('已删除')
  await loadMetric()
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.mono {
  font-family: var(--td-font-mono);
}
.mr-tag {
  margin-right: 4px;
}
.sql-preview {
  font-size: 11px;
  color: var(--td-text-3);
}
</style>
