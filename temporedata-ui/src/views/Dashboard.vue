<template>
  <div class="dashboard">
    <PageHeader title="监控总览" desc="TemporeData 大数据平台 · 一屏掌握数据资产与运行健康" />

    <!-- Stat Cards -->
    <div class="stat-cards">
      <div class="stat-card" v-for="card in statCards" :key="card.key" @click="goTo(card.key)">
        <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <!-- Charts Row 1: trend + datasource dist -->
    <el-row :gutter="16" class="charts-row">
      <el-col :xs="24" :lg="14">
        <div class="chart-container">
          <div class="chart-head">
            <h3>近24小时同步任务趋势</h3>
            <span class="chart-legend"><i class="dot dot-success"></i>成功 <i class="dot dot-danger"></i>失败</span>
          </div>
          <v-chart class="chart" :option="trendOption" autoresize />
        </div>
      </el-col>
      <el-col :xs="24" :lg="10">
        <div class="chart-container">
          <h3>数据源类型分布</h3>
          <v-chart class="chart" :option="distOption" autoresize />
        </div>
      </el-col>
    </el-row>

    <!-- Charts Row 2: status + issues -->
    <el-row :gutter="16" class="charts-row">
      <el-col :xs="24" :lg="10">
        <div class="chart-container">
          <h3>任务与质量状态</h3>
          <v-chart class="chart" :option="statusOption" autoresize />
        </div>
      </el-col>
      <el-col :xs="24" :lg="14">
        <div class="chart-container">
          <h3>异常快照</h3>
          <el-tabs v-model="issueTab" class="issue-tabs">
            <el-tab-pane :label="`同步失败 (${failedSyncs.length})`" name="sync">
              <el-table :data="failedSyncs" stripe size="small" empty-text="暂无失败任务" max-height="220">
                <el-table-column prop="taskName" label="任务" min-width="140" show-overflow-tooltip />
                <el-table-column prop="targetTable" label="目标表" min-width="120" show-overflow-tooltip />
                <el-table-column prop="lastRunTime" label="失败时间" width="150" />
                <el-table-column prop="errorMsg" label="错误信息" min-width="160" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>
            <el-tab-pane :label="`质量规则 FAIL (${failedRules.length})`" name="quality">
              <el-table :data="failedRules" stripe size="small" empty-text="全部规则通过" max-height="220">
                <el-table-column prop="ruleName" label="规则" min-width="140" show-overflow-tooltip />
                <el-table-column prop="tableName" label="表" min-width="130" show-overflow-tooltip />
                <el-table-column prop="columnName" label="字段" width="110" show-overflow-tooltip />
                <el-table-column prop="ruleType" label="规则类型" width="110">
                  <template #default="{ row }">
                    <el-tag size="small" effect="plain">{{ row.ruleType }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>

    <!-- Row 3: top tables + recent syncs -->
    <el-row :gutter="16" class="charts-row">
      <el-col :xs="24" :lg="10">
        <div class="chart-container">
          <h3>行数 TOP10 表</h3>
          <v-chart class="chart" :option="topTablesOption" autoresize />
        </div>
      </el-col>
      <el-col :xs="24" :lg="14">
        <div class="chart-container">
          <h3>最近同步记录</h3>
          <el-table :data="recentSyncs" stripe size="small" max-height="280">
            <el-table-column prop="taskName" label="任务" min-width="150" show-overflow-tooltip />
            <el-table-column prop="lastRunTime" label="运行时间" width="150" />
            <el-table-column prop="rowCount" label="同步行数" width="100" align="right">
              <template #default="{ row }">
                <span>{{ row.rowCount ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="statusTag(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import { dashboardApi } from '@/api/modules/dashboard'
import PageHeader from '@/components/PageHeader.vue'

use([
  CanvasRenderer,
  LineChart,
  PieChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const router = useRouter()

// ---- Stat Cards ----
const data = ref({})

const statCards = computed(() => [
  { key: 'datasource', label: '数据源', value: data.value.datasourceCount ?? 0, icon: 'Coin', bg: '#e8f1fe', color: '#1a6ff5', route: '/datasource' },
  { key: 'table', label: '已采集表', value: data.value.tableCount ?? 0, icon: 'Files', bg: '#ecfdf5', color: '#10b981', route: '/meta' },
  { key: 'row', label: '总数据量(行)', value: fmt(data.value.totalRows ?? 0), icon: 'Histogram', bg: '#fef3c7', color: '#f59e0b', route: '/meta' },
  { key: 'sync', label: '同步任务', value: data.value.syncTaskCount ?? 0, icon: 'Refresh', bg: '#ede9fe', color: '#8b5cf6', route: '/scheduler' },
  { key: 'quality', label: '质量规则', value: data.value.qualityRuleCount ?? 0, icon: 'CircleCheck', bg: '#fce7f3', color: '#ec4899', route: '/qualityrule' },
  { key: 'workflow', label: '作业流', value: data.value.workflowCount ?? 0, icon: 'Share', bg: '#e0f2fe', color: '#0ea5e9', route: '/workflow' },
  { key: 'api', label: 'API服务', value: data.value.apiCount ?? 0, icon: 'Link', bg: '#f3e8ff', color: '#a855f7', route: '/dataapi' }
])

function fmt(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

function goTo(card) {
  if (card.route) router.push(card.route)
}

// ---- Trend Chart ----
const trendOption = computed(() => {
  const trend = data.value.hourlyTrend || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: trend.map(t => t.hour),
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#9ca3af', fontSize: 10, interval: 3 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      axisLabel: { color: '#9ca3af', fontSize: 11 }
    },
    series: [
      {
        name: '成功',
        type: 'line',
        data: trend.map(t => t.success),
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { color: '#10b981', width: 2 },
        itemStyle: { color: '#10b981' }
      },
      {
        name: '失败',
        type: 'line',
        data: trend.map(t => t.failed),
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { color: '#ef4444', width: 2 },
        itemStyle: { color: '#ef4444' }
      }
    ]
  }
})

// ---- Datasource Type Distribution ----
const distOption = computed(() => {
  const dist = data.value.datasourceTypeDist || []
  const palette = ['#1a6ff5', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#0ea5e9', '#84cc16']
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: '0%', textStyle: { color: '#6b7280', fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['45%', '72%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      color: palette,
      data: dist.map(d => ({ value: d.value, name: d.name }))
    }]
  }
})

// ---- Status Distribution ----
const statusOption = computed(() => {
  const sync = data.value.syncStatus || []
  const quality = data.value.qualityStatus || []
  const syncMap = Object.fromEntries(sync.map(s => [s.name, s.value]))
  const qMap = Object.fromEntries(quality.map(q => [q.name, q.value]))
  return {
    tooltip: { trigger: 'axis' },
    legend: { bottom: '0%', textStyle: { color: '#6b7280', fontSize: 12 } },
    grid: { left: '3%', right: '4%', top: '6%', bottom: '14%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['READY', 'RUNNING', 'SUCCESS', 'FAILED', 'PASS', 'FAIL'],
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#9ca3af', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      axisLabel: { color: '#9ca3af', fontSize: 11 }
    },
    series: [
      {
        name: '同步任务',
        type: 'bar',
        barWidth: '32%',
        data: [syncMap.READY ?? 0, syncMap.RUNNING ?? 0, syncMap.SUCCESS ?? 0, syncMap.FAILED ?? 0, null, null],
        itemStyle: { borderRadius: [3, 3, 0, 0], color: '#3b82f6' }
      },
      {
        name: '质量规则',
        type: 'bar',
        barWidth: '32%',
        data: [null, null, null, null, qMap.PASS ?? 0, qMap.FAIL ?? 0],
        itemStyle: { borderRadius: [3, 3, 0, 0], color: '#10b981' }
      }
    ]
  }
})

// ---- Top Tables ----
const topTablesOption = computed(() => {
  const top = data.value.topTables || []
  const names = top.map(t => t.tableName).reverse()
  const values = top.map(t => t.rowCount).reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '6%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      axisLabel: { color: '#9ca3af', fontSize: 10 }
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#6b7280', fontSize: 11 }
    },
    series: [{
      type: 'bar',
      data: values,
      barWidth: '55%',
      itemStyle: { borderRadius: [0, 3, 3, 0], color: '#1a6ff5' },
      label: { show: true, position: 'right', color: '#9ca3af', fontSize: 10 }
    }]
  }
})

// ---- Tables ----
const failedSyncs = ref([])
const failedRules = ref([])
const recentSyncs = ref([])
const issueTab = ref('sync')

// ---- Fetch ----
onMounted(async () => {
  try {
    const res = await dashboardApi.get()
    if (res) {
      data.value = res
      failedSyncs.value = res.failedSyncs || []
      failedRules.value = res.failedRules || []
      recentSyncs.value = res.recentSyncs || []
    }
  } catch (e) {
    console.error('Failed to fetch dashboard data:', e)
  }
})

function statusTag(s) {
  return { SUCCESS: 'success', FAILED: 'danger', RUNNING: 'warning', READY: 'info' }[s] || 'info'
}
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.charts-row {
  margin-bottom: 16px;
}

.chart-container {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.chart-container h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px;
}

.chart {
  height: 260px;
}

.chart-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-legend {
  font-size: 12px;
  color: #9ca3af;
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin: 0 4px 0 8px;
}

.dot-success {
  background: #10b981;
}

.dot-danger {
  background: #ef4444;
}

.issue-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

@media (max-width: 1600px) {
  .stat-cards {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
