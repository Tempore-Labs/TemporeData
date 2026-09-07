<template>
  <div class="ha-page">
    <div class="page-header">
      <h2 class="page-title">高可用管理</h2>
      <div class="page-actions">
        <el-button :icon="Refresh" @click="fetchServers">刷新</el-button>
        <el-button type="warning" :icon="RefreshRight" @click="handleRecover">恢复作业</el-button>
      </div>
    </div>

    <!-- Server List -->
    <el-table :data="servers" v-loading="loading" stripe>
      <el-table-column prop="serverName" label="服务器名称" min-width="160" />
      <el-table-column prop="host" label="IP地址" width="140" />
      <el-table-column prop="port" label="端口" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastHeartbeat" label="最后心跳" width="170">
        <template #default="{ row }">
          <span :style="{ color: heartbeatColor(row.lastHeartbeat) }">{{ row.lastHeartbeat }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="registerTime" label="注册时间" width="170" />
    </el-table>

    <!-- HA Info -->
    <el-card class="info-card">
      <template #header><span class="card-title">高可用说明</span></template>
      <div class="ha-info">
        <p><strong>心跳间隔：</strong>30 秒</p>
        <p><strong>离线判定：</strong>90 秒无心跳自动标记为 OFFLINE</p>
        <p><strong>作业恢复：</strong>服务重启后自动将 RUNNING 状态作业标记为 FAILED</p>
        <p><strong>宕机恢复：</strong>检测到服务器离线后自动恢复其上的运行中作业</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { haApi } from '@/api/modules/ha'

const loading = ref(false)
const servers = ref([])

onMounted(() => { fetchServers() })

async function fetchServers() {
  loading.value = true
  try {
    servers.value = await haApi.getServers() || []
  } catch (e) {
    ElMessage.error('获取服务器列表失败')
  } finally { loading.value = false }
}

async function handleRecover() {
  try {
    await haApi.recover()
    ElMessage.success('作业恢复已触发')
    fetchServers()
  } catch (e) { ElMessage.error('恢复失败') }
}

function statusTag(status) {
  const map = { ACTIVE: 'success', STANDBY: 'warning', OFFLINE: 'danger' }
  return map[status] || 'info'
}

function heartbeatColor(time) {
  if (!time) return '#6b7280'
  const diff = Date.now() - new Date(time).getTime()
  if (diff < 60000) return '#10b981'
  if (diff < 90000) return '#f59e0b'
  return '#ef4444'
}
</script>

<style scoped>
.ha-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-actions { display: flex; gap: 8px; }
.info-card { margin-top: 0; }
.card-title { font-size: 15px; font-weight: 600; }
.ha-info p { margin: 8px 0; color: #4b5563; font-size: 14px; }
</style>