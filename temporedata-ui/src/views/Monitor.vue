<template>
  <div class="monitor-page">
    <div class="page-header">
      <h2 class="page-title">系统监控</h2>
      <el-button :icon="Refresh" @click="fetchAll">刷新</el-button>
    </div>

    <!-- System Overview Cards -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.key">
        <div class="stat-card">
          <div class="stat-icon" :style="{ background: card.bg }">
            <el-icon :size="24"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ card.active }} / {{ card.total }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Instance Monitor -->
    <el-card class="section-card">
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

    <!-- Cluster Monitor -->
    <el-card class="section-card" v-if="clusters.length">
      <template #header>
        <div class="section-header">
          <span class="section-title">集群监控</span>
          <el-select v-model="selectedCluster" placeholder="选择集群" style="width: 200px" @change="fetchClusterMonitor">
            <el-option v-for="c in clusters" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-radio-group v-model="timeType" size="small" @change="fetchClusterMonitor">
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
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh, Monitor, Connection, Odometer, Timer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { monitorApi } from '@/api/modules/monitor'
import { clusterApi } from '@/api/modules/cluster'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const systemData = ref({})
const clusters = ref([])
const selectedCluster = ref('')
const timeType = ref('1_HOUR')
const clusterData = ref([])
const instanceData = ref({})

const statCards = computed(() => [
  { key: 'cluster', label: '集群', icon: Monitor, bg: '#e0f2fe', active: systemData.value?.cluster?.active || 0, total: systemData.value?.cluster?.total || 0 },
  { key: 'datasource', label: '数据源', icon: Connection, bg: '#d1fae5', active: systemData.value?.datasource?.active || 0, total: systemData.value?.datasource?.total || 0 },
  { key: 'workflow', label: '作业流', icon: Odometer, bg: '#fef3c7', active: systemData.value?.workflow?.active || 0, total: systemData.value?.workflow?.total || 0 },
  { key: 'execution', label: '执行记录', icon: Timer, bg: '#ede9fe', active: systemData.value?.execution?.success || 0, total: systemData.value?.execution?.total || 0 },
])

const instanceStats = computed(() => [
  { key: 'total', label: '总实例数', value: instanceData.value?.total || 0, color: '#1f2937' },
  { key: 'running', label: '运行中', value: instanceData.value?.running || 0, color: '#f59e0b' },
  { key: 'success', label: '成功', value: instanceData.value?.success || 0, color: '#10b981' },
  { key: 'failed', label: '失败', value: instanceData.value?.failed || 0, color: '#ef4444' },
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
    { name: '网络读(KB/s)', type: 'line', data: clusterData.value.map(d => d.networkIoReadSpeed), smooth: true },
  ]
}))

onMounted(() => { fetchAll() })

async function fetchAll() {
  await Promise.all([fetchSystem(), fetchInstance(), fetchClusters()])
}

async function fetchSystem() {
  try { systemData.value = await monitorApi.getSystem() || {} } catch (e) { /* ignore */ }
}

async function fetchInstance() {
  try { instanceData.value = await monitorApi.getInstance() || {} } catch (e) { /* ignore */ }
}

async function fetchClusters() {
  try { clusters.value = await clusterApi.list() || [] } catch (e) { /* ignore */ }
}

async function fetchClusterMonitor() {
  if (!selectedCluster.value) return
  try {
    clusterData.value = await monitorApi.getCluster(selectedCluster.value, timeType.value) || []
  } catch (e) { ElMessage.error('获取集群监控失败') }
}
</script>

<style scoped>
.monitor-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.stat-row { margin-bottom: 0; }
.stat-card { display: flex; align-items: center; gap: 14px; padding: 16px; background: #fff; border-radius: 8px; border: 1px solid #e5e7eb; }
.stat-icon { width: 48px; height: 48px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-body { flex: 1; }
.stat-value { font-size: 20px; font-weight: 700; color: #1f2937; }
.stat-label { font-size: 13px; color: #6b7280; margin-top: 2px; }
.section-card { margin-top: 0; }
.section-title { font-size: 15px; font-weight: 600; }
.section-header { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.instance-stat { text-align: center; padding: 16px; }
.instance-value { font-size: 28px; font-weight: 700; }
.instance-label { font-size: 13px; color: #6b7280; margin-top: 4px; }
.monitor-chart { width: 100%; height: 350px; }
</style>