<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">计算</h2>
      <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
    </div>

    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限查看计算" />

    <template v-else>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="容器" name="containers">
          <DataTable :rows="containers" :columns="containerColumns" :state="state">
            <template #status="{ row }"><StatusBadge :value="CONTAINER_TONE[row.status] || 'unknown'" /></template>
          </DataTable>
        </el-tab-pane>

        <el-tab-pane label="Spark 作业" name="spark">
          <DataTable :rows="sparkJobs" :columns="sparkColumns" :state="state">
            <template #status="{ row }"><StatusBadge :value="ENGINE_TONE[row.status] || 'unknown'" /></template>
          </DataTable>
        </el-tab-pane>

        <el-tab-pane label="Flink 作业" name="flink">
          <DataTable :rows="flinkJobs" :columns="flinkColumns" :state="state">
            <template #status="{ row }"><StatusBadge :value="ENGINE_TONE[row.status] || 'unknown'" /></template>
          </DataTable>
        </el-tab-pane>
      </el-tabs>
    </template>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { CONTAINER_TONE, ENGINE_TONE } from '../types'
import { listComputeContainers, listEngineSpark, listEngineFlink } from '../api'

const activeTab = ref('containers')
const containers = ref([])
const sparkJobs = ref([])
const flinkJobs = ref([])
const { state, error, startLoading, finish, fail, deny } = usePageState()

const containerColumns = [
  { prop: 'name', label: '容器名称', width: 160 },
  { prop: 'type', label: '类型', width: 120 },
  { prop: 'image', label: '镜像', width: 200 },
  { slot: 'status', label: '状态', width: 110 },
  { prop: 'cpuCores', label: 'CPU核', width: 80, align: 'center' },
  { prop: 'memoryMb', label: '内存(MB)', width: 110, align: 'right' }
]
const engineBaseColumns = [
  { prop: 'name', label: '作业名称', width: 160 },
  { prop: 'mainClass', label: '主类', width: 220 },
  { prop: 'jarPath', label: 'JAR 路径', width: 220 },
  { slot: 'status', label: '状态', width: 110 }
]
const sparkColumns = [...engineBaseColumns]
const flinkColumns = [...engineBaseColumns.slice(0, 3), { prop: 'parallelism', label: '并行度', width: 90, align: 'center' }, ...engineBaseColumns.slice(3)]

async function loadAll() {
  startLoading()
  try {
    const settled = await Promise.allSettled([listComputeContainers(), listEngineSpark(), listEngineFlink()])
    containers.value = settled[0].status === 'fulfilled' ? (settled[0].value || []) : []
    sparkJobs.value = settled[1].status === 'fulfilled' ? (settled[1].value || []) : []
    flinkJobs.value = settled[2].status === 'fulfilled' ? (settled[2].value || []) : []
    const allEmpty = !containers.value.length && !sparkJobs.value.length && !flinkJobs.value.length
    finish(containers.value, allEmpty)
  } catch (e) { fail(e) }
}

onMounted(async () => { if (!hasPerm('compute:read')) { deny(); return } await loadAll() })
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-min-h { min-height: 120px; }
</style>