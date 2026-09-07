<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">集群详情</h2>
      <el-button size="small" @click="$router.back()">返回</el-button>
    </div>

    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限" />

    <template v-else>
      <el-descriptions v-if="cluster" :column="3" border size="small">
        <el-descriptions-item label="名称">{{ cluster.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ cluster.type }}</el-descriptions-item>
        <el-descriptions-item label="Master URL">{{ cluster.masterUrl }}</el-descriptions-item>
      </el-descriptions>

      <h3 class="v2-subtitle">节点</h3>
      <DataTable :rows="nodes" :columns="nodeColumns" :state="nodeState" />
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'
import DataTable from '@/components/base/DataTable.vue'
import { getCluster, listClusterNodes } from '../api'

const route = useRoute()
const cluster = ref(null)
const nodes = ref([])
const nodeColumns = [
  { prop: 'host', label: '主机地址', width: 200 },
  { prop: 'port', label: '端口', width: 100, align: 'center' },
  { prop: 'agentStatus', label: 'Agent 状态', width: 140 }
]
const { state, error, startLoading, finish, fail, deny } = usePageState()
const nodeState = usePageState()

onMounted(async () => {
  if (!hasPerm('cluster:read')) { deny(); return }
  const id = route.params.id
  startLoading()
  try {
    cluster.value = await getCluster(id)
    try { nodes.value = await listClusterNodes(id); nodeState.finish(nodes.value, nodes.value.length === 0) } catch (e) { nodeState.fail(e) }
    finish(cluster.value)
  } catch (e) { fail(e) }
})
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-subtitle { margin: 20px 0 8px; font-size: 15px; }
.v2-min-h { min-height: 120px; }
</style>