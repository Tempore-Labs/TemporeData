<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">作业执行</h2>
      <el-button size="small" @click="$router.back()">返回</el-button>
    </div>

    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限" />

    <template v-else-if="execution">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="ID">{{ execution.id }}</el-descriptions-item>
        <el-descriptions-item label="作业">{{ execution.jobId }}</el-descriptions-item>
        <el-descriptions-item label="状态"><StatusBadge :value="EXECUTION_TONE[(execution.status || '').toLowerCase()] ? execution.status.toLowerCase() : 'unknown'" /></el-descriptions-item>
        <el-descriptions-item label="执行器">{{ execution.executorType }}</el-descriptions-item>
        <el-descriptions-item label="Trace ID">{{ execution.traceId || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-if="emptyDetail" description="暂无更多执行信息" :image-size="80" />
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { EXECUTION_TONE } from '../routes'
import { getJobExecution } from '../api'

const route = useRoute()
const execution = ref(null)
const emptyDetail = ref(false)
const { state, error, startLoading, finish, fail, deny } = usePageState()

onMounted(async () => {
  if (!hasPerm('job:read')) { deny(); return }
  startLoading()
  try {
    execution.value = await getJobExecution(route.params.id, route.params.executionId)
    emptyDetail.value = !execution.value
    finish(execution.value, !execution.value)
  } catch (e) { fail(e) }
})
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-min-h { min-height: 120px; }
</style>