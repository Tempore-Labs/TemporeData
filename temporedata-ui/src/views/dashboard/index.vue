<template>
  <div class="dashboard">
    <!-- Header -->
    <PageHeader title="数据概览" :subtitle="todayLabel">
      <template #actions>
        <el-button size="small" @click="refresh">刷新</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="8" animated style="padding: 8px" />

    <template v-else-if="data">
      <!-- Stat cards -->
      <div class="stat-grid">
        <StatCard
          v-for="s in statCards"
          :key="s.label"
          :icon="s.icon"
          :value="s.value"
          :label="s.label"
          :sub="s.sub"
          :color="s.color"
          :color-soft="s.soft"
        />
      </div>

      <!-- Row 1: trend + datasource pie -->
      <div class="row row-3">
        <SectionCard title="24小时同步趋势" sub="成功 / 失败任务数" class="col-2">
          <EChart :option="trendOption" :height="220" />
        </SectionCard>
        <SectionCard title="数据源类型分布">
          <EChart :option="pieOption" :height="220" />
        </SectionCard>
      </div>

      <!-- Row 2: status bar + failed lists -->
      <div class="row row-3">
        <SectionCard title="任务与质量状态" sub="各状态任务数量对比" class="col-2">
          <EChart :option="barOption" :height="200" />
        </SectionCard>
        <SectionCard title="数据异常" sub="最近失败项">
          <div class="fail-list">
            <div v-for="f in failedList" :key="f.key" class="fail-item">
              <div>
                <div class="fail-name mono">{{ f.name }}</div>
                <div class="fail-reason">{{ f.reason }}</div>
              </div>
              <StatusBadge :status="f.status" :text="f.text" />
            </div>
            <EmptyState v-if="failedList.length === 0" title="暂无异常" :icon="'CircleCheck'" />
          </div>
        </SectionCard>
      </div>

      <!-- Row 3: top tables + recent syncs -->
      <div class="row row-5">
        <SectionCard title="Top 10 数据量表" sub="按行数排序" class="col-2">
          <EChart :option="topTableOption" :height="240" />
        </SectionCard>
        <SectionCard title="最近同步记录" sub="实时更新" class="col-3">
          <DataTable :data="recentSyncs">
            <el-table-column prop="taskName" label="任务名称" min-width="150">
              <template #default="{ row }">
                <span class="mono">{{ row.taskName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="lastRunTime" label="最近运行" min-width="140">
              <template #default="{ row }">
                <span class="mono text-muted">{{ row.lastRunTime }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="rowCount" label="行数" width="90" align="right">
              <template #default="{ row }">
                <span class="mono">{{ fmtNum(row.rowCount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <StatusBadge :status="row.status" />
              </template>
            </el-table-column>
          </DataTable>
        </SectionCard>
      </div>
    </template>

    <EmptyState v-else title="暂无数据" desc="后端概览接口未返回数据" :icon="'DataLine'" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useQuery } from '@/composables/useQuery'
import PageHeader from '@/components/base/PageHeader.vue'
import StatCard from '@/components/base/StatCard.vue'
import SectionCard from '@/components/base/SectionCard.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import EChart from '@/components/base/EChart.vue'
import { dashboardApi } from '@/api/dashboard'
import type { DashboardRes } from '@/types/dashboard'

const { data, isLoading: loading, refetch } = useQuery<DashboardRes>(['dashboard'], () =>
  dashboardApi.overview(),
  { staleTime: 30_000 },
)

const todayLabel = new Date().toISOString().slice(0, 10) + ' — 平台数据全景'

function fmtNum(n?: number): string {
  if (n == null) return '—'
  if (n >= 1e6) return (n / 1e6).toFixed(1) + 'M'
  if (n >= 1e3) return (n / 1e3).toFixed(1) + 'K'
  return String(n)
}

const statCards = computed(() => {
  const d = data.value
  if (!d) return []
  return [
    { label: '数据源', value: d.datasourceCount, sub: `${d.datasourceList?.length || 0} 个实例`, color: '#2563eb', soft: '#e8f1fe', icon: 'Coin' },
    { label: '数据表', value: fmtNum(d.tableCount), sub: '表级资产', color: '#10b981', soft: '#d1fae5', icon: 'Box' },
    { label: '同步任务', value: d.syncTaskCount, sub: `${d.syncSuccessCount} 成功 / ${d.syncFailCount} 失败`, color: '#f59e0b', soft: '#fef3c7', icon: 'Refresh' },
    { label: '质量规则', value: d.qualityRuleCount, sub: `${d.qualityPassCount} 通过 / ${d.qualityFailCount} 失败`, color: '#10b981', soft: '#d1fae5', icon: 'Aim' },
    { label: '工作流', value: d.workflowCount, sub: '调度工作流', color: '#8b5cf6', soft: '#ede9fe', icon: 'Tickets' },
    { label: '接口服务', value: d.apiCount, sub: `累计调用 ${fmtNum(d.apiCallTotal)}`, color: '#2563eb', soft: '#e8f1fe', icon: 'Connection' },
    { label: '字段总量', value: fmtNum(d.totalRows), sub: '数据行总量', color: '#8b5cf6', soft: '#ede9fe', icon: 'Grid' },
    { label: '失败任务', value: d.failedSyncs?.length || 0, sub: '待处理', color: '#dc2626', soft: '#fee2e2', icon: 'Warning' },
  ]
})

const trendOption = computed<echarts.EChartsOption>(() => {
  const list = data.value?.hourlyTrend || []
  return {
    grid: { top: 24, right: 16, left: 8, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: list.map((i) => i.hour), axisTick: { show: false }, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#94a3b8', fontSize: 10, interval: 3 } },
    yAxis: { type: 'value', axisTick: { show: false }, axisLine: { show: false }, axisLabel: { color: '#94a3b8', fontSize: 10 }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: [
      { name: '成功', type: 'line', smooth: true, data: list.map((i) => i.success), showSymbol: false, lineStyle: { width: 2, color: '#2563eb' }, areaStyle: { color: 'rgba(37,99,235,0.08)' } },
      { name: '失败', type: 'line', smooth: true, data: list.map((i) => i.failed), showSymbol: false, lineStyle: { width: 2, color: '#dc2626' }, areaStyle: { color: 'rgba(220,38,38,0.06)' } },
    ],
    legend: { top: 0, right: 0, textStyle: { fontSize: 11, color: '#64748b' }, icon: 'circle', itemWidth: 8, itemHeight: 8 },
  }
})

const PIE_COLORS = ['#2563eb', '#10b981', '#f59e0b', '#8b5cf6', '#64748b']
const pieOption = computed<echarts.EChartsOption>(() => {
  const list = data.value?.datasourceTypeDist || []
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [
      {
        type: 'pie',
        radius: ['52%', '76%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 3, borderColor: '#fff', borderWidth: 1 },
        label: { show: false },
        emphasis: { label: { show: false } },
        data: list.map((s, i) => ({ name: s.name, value: s.value, itemStyle: { color: PIE_COLORS[i % PIE_COLORS.length] } })),
      },
    ],
  }
})

const barOption = computed<echarts.EChartsOption>(() => {
  const sync = data.value?.syncStatus || []
  const qual = data.value?.qualityStatus || []
  const names = [...new Set([...sync.map((s) => s.name), ...qual.map((s) => s.name)])]
  return {
    grid: { top: 32, right: 16, left: 8, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis' },
    legend: { top: 0, right: 0, textStyle: { fontSize: 11, color: '#64748b' }, icon: 'circle', itemWidth: 8, itemHeight: 8 },
    xAxis: { type: 'category', data: names, axisTick: { show: false }, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#64748b', fontSize: 11 } },
    yAxis: { type: 'value', axisTick: { show: false }, axisLine: { show: false }, axisLabel: { color: '#94a3b8', fontSize: 10 }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: [
      { name: '任务同步', type: 'bar', barMaxWidth: 18, itemStyle: { color: '#2563eb', borderRadius: [4, 4, 0, 0] }, data: names.map((n) => sync.find((s) => s.name === n)?.value || 0) },
      { name: '质量检查', type: 'bar', barMaxWidth: 18, itemStyle: { color: '#10b981', borderRadius: [4, 4, 0, 0] }, data: names.map((n) => qual.find((s) => s.name === n)?.value || 0) },
    ],
  }
})

const topTableOption = computed<echarts.EChartsOption>(() => {
  const list = (data.value?.topTables || []).slice(0, 10).reverse()
  return {
    grid: { top: 8, right: 32, left: 8, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis', formatter: (p: any) => `${p[0].name}: ${fmtNum(p[0].value)} 行` },
    xAxis: { type: 'value', axisTick: { show: false }, axisLine: { show: false }, axisLabel: { color: '#94a3b8', fontSize: 10, formatter: (v: number) => fmtNum(v) }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    yAxis: { type: 'category', data: list.map((t) => t.tableName), axisTick: { show: false }, axisLine: { show: false }, axisLabel: { color: '#475569', fontSize: 10, fontFamily: 'JetBrains Mono, monospace' } },
    series: [
      { type: 'bar', barMaxWidth: 12, data: list.map((t) => t.rowCount), itemStyle: { color: '#2563eb', borderRadius: [0, 4, 4, 0] }, label: { show: true, position: 'right', fontSize: 10, color: '#64748b', formatter: (p: any) => fmtNum(p.value) } },
    ],
  }
})

const recentSyncs = computed(() => data.value?.recentSyncs || [])
const failedList = computed(() => {
  const out: { key: string; name: string; reason: string; status: string; text: string }[] = []
  ;(data.value?.failedSyncs || []).forEach((f, i) =>
    out.push({ key: `s${i}`, name: f.taskName, reason: f.reason || '同步失败', status: 'failed', text: '失败' }),
  )
  ;(data.value?.failedRules || []).forEach((f, i) =>
    out.push({ key: `r${i}`, name: f.ruleName, reason: f.datasetName || '质量规则', status: 'warn', text: '警告' }),
  )
  return out.slice(0, 8)
})

function refresh() {
  refetch()
}
</script>

<style scoped>
.dashboard {
  padding: 24px;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
@media (max-width: 1600px) {
  .stat-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
.row {
  display: grid;
  gap: 16px;
  margin-bottom: 16px;
}
.row-3 {
  grid-template-columns: repeat(3, 1fr);
}
.row-5 {
  grid-template-columns: repeat(5, 1fr);
}
.col-2 {
  grid-column: span 2;
}
.col-3 {
  grid-column: span 3;
}
.fail-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 240px;
  overflow: auto;
}
.fail-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--td-border-light);
}
.fail-item:last-child {
  border-bottom: none;
}
.fail-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--td-text-1);
}
.fail-reason {
  font-size: 10px;
  color: var(--td-text-4);
  margin-top: 2px;
}
.mono {
  font-family: var(--td-font-mono);
}
.text-muted {
  color: var(--td-text-4);
}
</style>
