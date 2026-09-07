<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">集群</h2>
      <el-button v-if="canWrite" type="primary" @click="openCreate">新建集群</el-button>
    </div>

    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-empty v-else-if="state === 'empty'" description="暂无集群" :image-size="80" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限查看集群" />

    <DataTable v-else :rows="rows" :columns="columns" :state="state" :loading="state === 'loading'" @row-click="onRow">
      <template #status="{ row }"><StatusBadge :value="CLUSTER_TONE[row.status] || 'unknown'" /></template>
    </DataTable>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { CLUSTER_TONE } from '../types'
import { listClusters, createCluster } from '../api'

const router = useRouter()
const canWrite = hasPerm('cluster:write')
const columns = [
  { prop: 'name', label: '集群名称', width: 180 },
  { prop: 'type', label: '类型', width: 120 },
  { prop: 'masterUrl', label: 'Master URL', width: 240 },
  { slot: 'status', label: '状态', width: 120 },
  { prop: 'nodeCount', label: '节点数', width: 90, align: 'center' }
]
const { state, error, startLoading, finish, fail, deny } = usePageState()
let rows = []

async function load() {
  startLoading()
  try { rows = await listClusters(); finish(rows, rows.length === 0) } catch (e) { fail(e) }
}
function onRow(row) { router.push(`/v2/clusters/${row.id}`).catch(() => {}) }
async function openCreate() {
  if (!canWrite) { ElMessage.warning('缺少 cluster:write 权限'); return }
  await createCluster({ name: 'demo-' + Date.now() }).catch(() => {})
  load()
}

onMounted(async () => { if (!hasPerm('cluster:read')) { deny(); return } await load() })
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-min-h { min-height: 120px; }
</style>