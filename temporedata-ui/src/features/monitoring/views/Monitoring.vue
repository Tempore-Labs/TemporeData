<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">系统监控</h2>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限查看监控" />

    <div v-else>
      <!-- System overview cards -->
      <el-row :gutter="16">
        <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.key">
          <MetricCard :label="card.label" :value="card.active + ' / ' + card.total" />
        </el-col>
      </el-row>

      <!-- Instance running stats -->
      <el-card class="section-card" shadow="never">
        <template #header><span class="section-title">实例运行状态</span></template>
        <el-row :gutter="16">
          <el-col :xs="12" :sm="6" v-for="item in instanceStats" :key="item.key">
            <div class="instance-stat">
              <div class="instance-value" :style="{ color: item.color }">{{ item.value }}</div>
              <div class="instance-label">{{ item.label }}</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- Cluster monitor -->
      <el-card class="section-card" shadow="never" v-if="clusters.length">
        <template #header>
          <div class="section-header">
            <span class="section-title">集群监控</span>
            <el-select v-model="selectedCluster" placeholder="选择集群" style="width: 200px" @change="loadClusterMetric">
              <el-option v-for="c in clusters" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-radio-group v-model="timeType" size="small" @change="loadClusterMetric">
              <el-radio-button value="30_MIN">30分</el-radio-button>
              <el-radio-button value="1_HOUR">1小时</el-radio-button>
              <el-radio-button value="6_HOUR">6小时</el-radio-button>
              <el-radio-button value="1_DAY">1天</el-radio-button>
              <el-radio-button value="7_DAY">7天</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <v-chart v-if="clusterData.length" :option="clusterChartOption" autoresize class="monitor-chart" />
        <el-empty v-else description="请选择集群查看监控数据" />
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'
import MetricCard from '@/components/base/MetricCard.vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { getSystemMetric, getInstanceMetric, listMonitorClusters, getClusterMetric } from '../api'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const { state, error, startLoading, finish, fail, deny } = usePageState()
const systemData = ref({})
const clusters = ref([])
const selectedCluster = ref('')
const timeType = ref('1_HOUR')
const clusterData = ref([])
const instanceData = ref({})

const statCards = computed(() => [
  { key: 'cluster', label: '集群', active: systemData.value?.cluster?.active || 0, total: systemData.value?.cluster?.total || 0 },
  { key: 'datasource', label: '数据源', active: systemData.value?.datasource?.active || 0, total: systemData.value?.datasource?.total || 0 },
  { key: 'workflow', label: '作业流', active: systemData.value?.workflow?.active || 0, total: systemData.value?.workflow?.total || 0 },
  { key: 'execution', label: '执行记录', active: systemData.value?.execution?.success || 0, total: systemData.value?.execution?.total || 0 }
])

const instanceStats = computed(() => [
  { key: 'total', label: '总实例数', value: instanceData.value?.total || 0, color: '#1f2937' },
  { key: 'running', label: '运行中', value: instanceData.value?.running || 0, color: '#f59e0b' },
  { key: 'success', label: '成功', value: instanceData.value?.success || 0, color: '#10b981' },
  { key: 'failed', label: '失败', value: instanceData.value?.failed || 0, color: '#ef4444' }
])

const clusterChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['CPU %', '内存(GB)', '磁盘读(MB/s)', '网络读(KB/s)'], bottom: 0 },
  grid: { left: 50, right: 30, top: 20, bottom: 40 },
  xAxis: { type: 'category', data: clusterData.value.map(d => d.time) },
  yAxis: { type: 'value' },
  series: [
    { name: 'CPU %', type: 'line', data: clusterData.value.map(d => d.cpuPercent), smooth: true },
    { name: '内存(GB)', type: 'line', data: clusterData.value.map(d => d.usedMemorySize), smooth: true },
    { name: '磁盘读(MB/s)', type: 'line', data: clusterData.value.map(d => d.diskIoReadSpeed), smooth: true },
    { name: '网络读(KB/s)', type: 'line', data: clusterData.value.map(d => d.networkIoReadSpeed), smooth: true }
  ]
}))

async function load() {
  startLoading()
  try {
    const settled = await Promise.allSettled([getSystemMetric(), getInstanceMetric(), listMonitorClusters()])
    systemData.value = settled[0].status === 'fulfilled' ? (settled[0].value || {}) : {}
    instanceData.value = settled[1].status === 'fulfilled' ? (settled[1].value || {}) : {}
    clusters.value = settled[2].status === 'fulfilled' ? (settled[2].value || []) : []
    finish(clusters.value, clusters.value.length === 0)
  } catch (e) { fail(e) }
}

async function loadClusterMetric() {
  if (!selectedCluster.value) return
  try { clusterData.value = await getClusterMetric(selectedCluster.value) || [] } catch (e) { ElMessage.error('获取集群监控失败') }
}

onMounted(async () => { if (!hasPerm('metric:read')) { deny(); return } await load() })
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-min-h { min-height: 120px; }
.section-card { margin-top: 16px; }
.section-title { font-size: 15px; font-weight: 600; }
.section-header { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.instance-stat { text-align: center; padding: 8px; }
.instance-value { font-size: 26px; font-weight: 700; }
.instance-label { font-size: 13px; color: #6b7280; margin-top: 4px; }
.monitor-chart { width: 100%; height: 350px; }
</style>