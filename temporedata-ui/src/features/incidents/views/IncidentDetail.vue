<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">事件详情</h2>
      <el-button size="small" @click="$router.back()">返回</el-button>
    </div>

    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限" />

    <template v-else-if="incident">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="ID">{{ incident.id }}</el-descriptions-item>
        <el-descriptions-item label="事件类型">{{ incident.eventType }}</el-descriptions-item>
        <el-descriptions-item label="级别"><StatusBadge :value="INCIDENT_LEVEL_TONE[incident.level] || 'unknown'" /></el-descriptions-item>
        <el-descriptions-item label="状态"><StatusBadge :value="INCIDENT_TONE[incident.status] || incident.status || 'unknown'" /></el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ incident.createTime }}</el-descriptions-item>
      </el-descriptions>

      <h3 class="v2-subtitle">事件内容</h3>
      <el-input type="textarea" :model-value="incident.message || ''" :rows="3" readonly />

      <div class="v2-actions" v-if="canOperate">
        <el-button @click="onAck">确认</el-button>
        <el-button type="danger" @click="onResolve">解决</el-button>
      </div>
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { INCIDENT_TONE, INCIDENT_LEVEL_TONE } from '../types'
import { getIncident, ackIncident, resolveIncident } from '../api'

const route = useRoute()
const incident = ref(null)
const { state, error, startLoading, finish, fail, deny } = usePageState()
const canOperate = hasPerm('incident:operate')

async function load() {
  startLoading()
  try {
    incident.value = await getIncident(route.params.id)
    finish(incident.value, !incident.value)
  } catch (e) { fail(e) }
}

async function onAck() {
  if (!canOperate) { ElMessage.warning('缺少 incident:operate 权限'); return }
  try { await ackIncident(route.params.id); ElMessage.success('事件已确认'); load() } catch (e) { ElMessage.error(e?.message || '操作失败') }
}

// Resolve is HIGH_RISK (permissions/HIGH_RISK) -> explicit confirm before executing.
async function onResolve() {
  if (!canOperate) { ElMessage.warning('缺少 incident:operate 权限'); return }
  const confirmed = await ElMessageBox.confirm('解决该事件为高风险操作，是否继续？', '高风险确认', { type: 'warning' }).catch(() => false)
  if (!confirmed) return
  try { await resolveIncident(route.params.id); ElMessage.success('事件已解决'); load() } catch (e) { ElMessage.error(e?.message || '操作失败') }
}

onMounted(async () => { if (!hasPerm('incident:read')) { deny(); return } await load() })
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-subtitle { margin: 20px 0 8px; font-size: 15px; }
.v2-actions { margin-top: 16px; }
.v2-min-h { min-height: 120px; }
</style>