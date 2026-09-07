<template>
  <div class="indicator-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">数据指标</h2>
        <div class="page-desc">统一指标口径定义与实时取数 · 支持一键执行、调度任务与运行历史</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">新增指标</el-button>
    </div>

    <!-- Stats -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background:#e8f1fe;color:#1a6ff5"><el-icon><TrendCharts /></el-icon></div>
        <div>
          <div class="stat-value">{{ stats.total || indicators.length }}</div>
          <div class="stat-label">指标总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#ecfdf5;color:#10b981"><el-icon><CircleCheck /></el-icon></div>
        <div>
          <div class="stat-value">{{ successRuns }}</div>
          <div class="stat-label">成功执行</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#fee2e2;color:#ef4444"><el-icon><CircleClose /></el-icon></div>
        <div>
          <div class="stat-value">{{ failedRuns }}</div>
          <div class="stat-label">执行失败</div>
        </div>
      </div>
    </div>

    <!-- Indicator table -->
    <div class="table-card">
      <el-table :data="indicators" v-loading="loading" stripe>
        <el-table-column prop="name" label="指标名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="code" label="编码" width="120" show-overflow-tooltip />
        <el-table-column prop="type" label="口径" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.type)" effect="light">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最新运行" min-width="150">
          <template #default="{ row }">
            <div v-if="row.latestStatus" class="latest-run">
              <el-tag size="small" :type="row.latestStatus === 'SUCCESS' ? 'success' : 'danger'" effect="light">
                {{ row.latestStatus === 'SUCCESS' ? '成功' : '失败' }}
              </el-tag>
              <span class="latest-value">{{ row.latestValue ?? '-' }}<span v-if="row.unit" class="unit-text">{{ row.unit }}</span></span>
              <div class="latest-time">{{ row.latestExecuteTime?.replace('T', ' ').slice(0, 19) }}</div>
            </div>
            <span v-else class="muted">未执行</span>
          </template>
        </el-table-column>
        <el-table-column prop="theme" label="主题" width="100" show-overflow-tooltip />
        <el-table-column prop="owner" label="负责人" width="90" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 'ENABLED'" size="small" @change="v => toggleStatus(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="调度" width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.scheduleEnabled && row.status === 'ENABLED'" size="small" type="warning" effect="light">{{ row.scheduleCron }}</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" link :icon="VideoPlay" :loading="executingId === row.id" @click="handleExecute(row)">执行</el-button>
            <el-button size="small" type="primary" link :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该指标及运行历史？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
          v-model:current-page="page" v-model:page-size="size" :page-sizes="[10, 20, 50]"
          @current-change="fetchList" @size-change="fetchList" />
      </div>
    </div>

    <!-- Execute result dialog -->
    <el-dialog v-model="resultVisible" title="指标执行结果" width="520px" destroy-on-close>
      <div v-if="lastRun" class="result-body">
        <div class="result-hero" :class="lastRun.status === 'SUCCESS' ? 'is-success' : 'is-failed'">
          <div class="result-value">
            {{ lastRun.resultValue ?? '-' }}
            <span class="result-unit">{{ lastRun.unit || '' }}</span>
          </div>
          <div class="result-status">
            <el-tag :type="lastRun.status === 'SUCCESS' ? 'success' : 'danger'">
              {{ lastRun.status === 'SUCCESS' ? '执行成功' : '执行失败' }}
            </el-tag>
          </div>
        </div>
        <el-descriptions :column="2" border size="small" class="result-meta">
          <el-descriptions-item label="执行时间">{{ lastRun.executeTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ lastRun.durationMs != null ? lastRun.durationMs + ' ms' : '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-alert v-if="lastRun.status === 'FAILED'" :title="lastRun.errorMsg || '未知错误'" type="error" :closable="false" show-icon class="result-error" />
      </div>
    </el-dialog>

    <!-- Detail drawer: overview + trend + lineage + history -->
    <el-drawer v-model="detailVisible" :title="'指标详情 - ' + (currentIndicator?.name || '')" size="860px" destroy-on-close>
      <el-tabs v-model="detailTab" v-loading="detailLoading">
        <el-tab-pane label="概览" name="overview">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="名称">{{ currentIndicator?.name }}</el-descriptions-item>
            <el-descriptions-item label="编码">{{ currentIndicator?.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="口径类型">{{ typeLabel(currentIndicator?.type) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag size="small" :type="currentIndicator?.status === 'ENABLED' ? 'success' : 'info'">
                {{ currentIndicator?.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="版本">{{ currentIndicator?.version || '-' }}</el-descriptions-item>
            <el-descriptions-item label="主题域">{{ currentIndicator?.theme || '-' }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ currentIndicator?.owner || '-' }}</el-descriptions-item>
            <el-descriptions-item label="数据源">{{ datasourceName(currentIndicator?.datasourceId) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="单位">{{ currentIndicator?.unit || '-' }}</el-descriptions-item>
            <el-descriptions-item label="调度">
              <template v-if="currentIndicator?.scheduleEnabled && currentIndicator?.status === 'ENABLED'">
                <el-tag size="small" type="warning">{{ currentIndicator?.scheduleCron }}</el-tag>
              </template>
              <span v-else class="muted">未启用</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentIndicator?.createTime?.replace('T', ' ').slice(0, 19) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ currentIndicator?.updateTime?.replace('T', ' ').slice(0, 19) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="最近运行" :span="2">
              <template v-if="currentIndicator?.latestStatus">
                <el-tag size="small" :type="currentIndicator.latestStatus === 'SUCCESS' ? 'success' : 'danger'">
                  {{ currentIndicator.latestStatus === 'SUCCESS' ? '成功' : '失败' }}
                </el-tag>
                <span class="overview-value">{{ currentIndicator.latestValue ?? '-' }}{{ currentIndicator.unit || '' }}</span>
                <span class="muted"> · {{ currentIndicator.latestExecuteTime?.replace('T', ' ').slice(0, 19) }}</span>
              </template>
              <span v-else class="muted">从未执行</span>
            </el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ currentIndicator?.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="查询SQL" :span="2">
              <pre class="sql-block">{{ currentIndicator?.querySql }}</pre>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="趋势" name="trend">
          <div v-if="runs.length > 1" class="trend-chart">
            <VChart :option="trendOption" autoresize />
          </div>
          <el-empty v-else description="执行历史不足 2 次，暂无可视化趋势" :image-size="80" />
        </el-tab-pane>

        <el-tab-pane label="血缘" name="lineage">
          <div class="lineage-section">
            <div class="lineage-title">依赖表（SQL 引用）</div>
            <div v-if="lineage.tables?.length" class="table-tags">
              <el-tag v-for="t in lineage.tables" :key="t" size="small" type="primary" effect="plain">{{ t }}</el-tag>
            </div>
            <span v-else class="muted">未解析出依赖表（SQL 需可解析的 SELECT / WITH 语句）</span>
          </div>
          <div class="lineage-section">
            <div class="lineage-title">上游指标（引用相同表）</div>
            <el-table v-if="lineage.upstream?.length" :data="lineage.upstream" size="small" stripe>
              <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
              <el-table-column prop="code" label="编码" width="150" show-overflow-tooltip />
              <el-table-column prop="type" label="口径" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="typeTag(row.type)" effect="light">{{ typeLabel(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button size="small" type="primary" link @click="jumpDetail(row)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
            <span v-else class="muted">暂无上游指标</span>
          </div>
        </el-tab-pane>

        <el-tab-pane label="运行历史" name="history">
          <el-table :data="runs" v-loading="runsLoading" stripe size="small" max-height="480">
            <el-table-column prop="executeTime" label="执行时间" width="170" />
            <el-table-column prop="resultValue" label="结果值" width="120">
              <template #default="{ row }">
                <strong>{{ row.resultValue ?? '-' }}</strong> <span class="unit-text">{{ row.unit || '' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="durationMs" label="耗时(ms)" width="100" align="right" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'SUCCESS' ? 'success' : 'danger'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="errorMsg" label="错误信息" min-width="200" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <!-- Add/Edit dialog -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑指标' : '新增指标'" width="660px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" label-position="right">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：当日订单总量" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="全局唯一编码，如 order_daily_count" />
        </el-form-item>
        <el-form-item label="口径类型" prop="type">
          <el-select v-model="form.type" placeholder="选择统计口径" style="width: 100%">
            <el-option label="计数 COUNT" value="COUNT" />
            <el-option label="求和 SUM" value="SUM" />
            <el-option label="平均 AVG" value="AVG" />
            <el-option label="比率 RATIO" value="RATIO" />
            <el-option label="自定义 CUSTOM" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源" prop="datasourceId">
          <el-select v-model="form.datasourceId" placeholder="选择指标取数数据源" filterable style="width: 100%">
            <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="查询SQL" prop="querySql">
          <el-input v-model="form.querySql" type="textarea" :rows="4" placeholder="返回单值（取第一行第一列），如：SELECT COUNT(*) FROM fact_sales" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="版本" prop="version">
            <el-input v-model="form.version" placeholder="如 v1.0" />
          </el-form-item>
          <el-form-item label="主题域" prop="theme">
            <el-input v-model="form.theme" placeholder="如 销售域 / 财务域" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="负责人" prop="owner">
            <el-input v-model="form.owner" placeholder="负责人姓名" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="启用" value="ENABLED" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="调度Cron" prop="scheduleCron">
            <el-input v-model="form.scheduleCron" placeholder="如 0 0 9 * * ? （每天9点）" />
          </el-form-item>
          <el-form-item label="启用调度">
            <el-switch v-model="form.scheduleEnabled" />
          </el-form-item>
        </div>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="如：笔 / 万元 / %" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="指标业务口径说明，如：统计当日已支付订单数量（不含取消）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, VideoPlay, View, TrendCharts, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { indicatorApi } from '@/api/modules/indicator'
import { datasourceApi } from '@/api/modules/datasource'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const loading = ref(false)
const indicators = ref([])
const datasources = ref([])
const executingId = ref('')
const stats = ref({ totalRuns: 0, successRuns: 0, failedRuns: 0 })
const page = ref(1)
const size = ref(20)
const total = ref(0)

// Result dialog
const resultVisible = ref(false)
const lastRun = ref(null)

// Detail drawer
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailTab = ref('overview')
const runsLoading = ref(false)
const runs = ref([])
const lineage = ref({ tables: [], upstream: [] })
const currentIndicator = ref(null)

// Form
const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const currentEditId = ref('')

const successRuns = computed(() => stats.value.successRuns)
const failedRuns = computed(() => stats.value.failedRuns)

const trendOption = computed(() => {
  const asc = [...runs.value].reverse()
  const times = asc.map(r => r.executeTime?.replace('T', ' ').slice(5, 16) || '')
  const values = asc.map(r => (r.resultValue != null && !isNaN(Number(r.resultValue)) ? Number(r.resultValue) : null))
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['结果值'], top: 0 },
    grid: { left: 40, right: 16, top: 32, bottom: 28 },
    xAxis: { type: 'category', data: times, axisLabel: { rotate: 30, fontSize: 10 } },
    yAxis: { type: 'value' },
    series: [{
      name: '结果值',
      type: 'line',
      smooth: true,
      connectNulls: false,
      data: values,
      symbolSize: 8,
      itemStyle: {
        color: (p) => (asc[p.dataIndex]?.status === 'SUCCESS' ? '#10b981' : '#ef4444')
      },
      lineStyle: { color: '#1a6ff5', width: 2 },
      areaStyle: { opacity: 0.06 }
    }]
  }
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  type: [{ required: true, message: '请选择口径类型', trigger: 'change' }],
  datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  querySql: [{ required: true, message: '请输入查询SQL', trigger: 'blur' }]
}

const defaultForm = () => ({
  name: '',
  code: '',
  type: '',
  description: '',
  querySql: '',
  datasourceId: '',
  unit: '',
  version: '',
  theme: '',
  owner: '',
  status: 'ENABLED',
  scheduleCron: '',
  scheduleEnabled: false
})

const form = ref(defaultForm())

onMounted(async () => {
  fetchList()
  fetchStats()
  try { datasources.value = await datasourceApi.list() || [] } catch (e) { /* ignore */ }
})

async function fetchStats() {
  try {
    stats.value = await indicatorApi.stats() || { totalRuns: 0, successRuns: 0, failedRuns: 0 }
  } catch (e) { /* keep defaults */ }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await indicatorApi.list(page.value - 1, size.value) || { records: [], total: 0 }
    indicators.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    ElMessage.error('获取指标列表失败')
  } finally {
    loading.value = false
  }
}

function datasourceName(id) {
  return datasources.value.find(d => d.id === id)?.name || ''
}

function typeLabel(t) {
  return { COUNT: '计数', SUM: '求和', AVG: '平均', RATIO: '比率', CUSTOM: '自定义' }[t] || t
}

function typeTag(t) {
  return { COUNT: '', SUM: 'success', AVG: 'warning', RATIO: 'danger', CUSTOM: 'info' }[t] || 'info'
}

async function handleExecute(row) {
  executingId.value = row.id
  try {
    const run = await indicatorApi.execute(row.id)
    lastRun.value = run
    resultVisible.value = true
    if (run.status === 'SUCCESS') {
      ElMessage.success(`执行成功: ${run.resultValue}${run.unit || ''}`)
    } else {
      ElMessage.error('执行失败: ' + (run.errorMsg || '未知错误'))
    }
    fetchList()
    fetchStats()
  } catch (e) {
    ElMessage.error('执行请求失败')
  } finally {
    executingId.value = ''
  }
}

async function openDetail(row) {
  currentIndicator.value = row
  detailVisible.value = true
  detailTab.value = 'overview'
  detailLoading.value = true
  lineage.value = { tables: [], upstream: [] }
  try {
    lineage.value = await indicatorApi.lineage(row.id) || { tables: [], upstream: [] }
  } catch (e) { /* ignore */ }
  await loadRuns(row)
  detailLoading.value = false
}

async function loadRuns(row) {
  runsLoading.value = true
  try {
    runs.value = await indicatorApi.runs(row.id) || []
  } catch (e) {
    ElMessage.error('获取运行历史失败')
  } finally {
    runsLoading.value = false
  }
}

function jumpDetail(row) {
  const found = indicators.value.find(i => i.id === row.id)
  openDetail(found || row)
}

async function toggleStatus(row, enabled) {
  try {
    await indicatorApi.update(row.id, {
      name: row.name,
      code: row.code,
      type: row.type,
      description: row.description,
      querySql: row.querySql,
      datasourceId: row.datasourceId,
      unit: row.unit,
      version: row.version,
      theme: row.theme,
      owner: row.owner,
      status: enabled ? 'ENABLED' : 'DISABLED',
      scheduleCron: row.scheduleCron,
      scheduleEnabled: row.scheduleEnabled
    })
    ElMessage.success(enabled ? '已启用' : '已停用')
    fetchList()
  } catch (e) {
    ElMessage.error('状态切换失败')
    fetchList()
  }
}

function openAdd() {
  isEdit.value = false
  form.value = defaultForm()
  formVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  currentEditId.value = row.id
  form.value = {
    name: row.name,
    code: row.code,
    type: row.type,
    description: row.description,
    querySql: row.querySql,
    datasourceId: row.datasourceId,
    unit: row.unit,
    version: row.version || '',
    theme: row.theme || '',
    owner: row.owner || '',
    status: row.status || 'ENABLED',
    scheduleCron: row.scheduleCron || '',
    scheduleEnabled: !!row.scheduleEnabled
  }
  formVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await indicatorApi.update(currentEditId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await indicatorApi.create(form.value)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    await fetchList()
    await fetchStats()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || (isEdit.value ? '更新失败' : '新增失败'))
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    await indicatorApi.delete(id)
    ElMessage.success('删除成功')
    await fetchList()
    await fetchStats()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.indicator-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.page-desc {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  max-width: 720px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.stat-label {
  font-size: 12px;
  color: #9ca3af;
}

.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.latest-run {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.latest-value {
  font-weight: 600;
  color: #1f2937;
}

.latest-time {
  font-size: 11px;
  color: #9ca3af;
  width: 100%;
}

.muted {
  color: #9ca3af;
  font-size: 12px;
}

.unit-text {
  font-size: 12px;
  color: #9ca3af;
  margin-left: 2px;
}

.result-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 14px;
}

.result-hero.is-success {
  background: #f0fdf4;
}

.result-hero.is-failed {
  background: #fef2f2;
}

.result-value {
  font-size: 34px;
  font-weight: 700;
  color: #1f2937;
}

.result-unit {
  font-size: 15px;
  color: #6b7280;
  margin-left: 6px;
}

.result-error {
  margin-top: 12px;
}

.trend-chart {
  height: 340px;
  width: 100%;
}

.lineage-section {
  margin-bottom: 18px;
}

.lineage-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.table-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.overview-value {
  font-weight: 600;
  margin-left: 8px;
  color: #1f2937;
}

.sql-block {
  margin: 0;
  padding: 10px;
  background: #f8fafc;
  border-radius: 6px;
  font-family: Menlo, Consolas, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow: auto;
  color: #374151;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: 1fr; }
  .form-row { grid-template-columns: 1fr; }
}
</style>