<template>
  <section class="v2-page">
    <h2 class="v2-page-title">总览 (v2.0 shell)</h2>
    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限查看总览" />
    <div v-else>
      <el-row :gutter="12">
        <el-col :span="6"><MetricCard label="集群" :value="overview?.clusters ?? '-'" /></el-col>
        <el-col :span="6"><MetricCard label="作业" :value="overview?.jobs ?? '-'" accent /></el-col>
        <el-col :span="6"><MetricCard label="进行中执行" :value="overview?.running?? 0" /></el-col>
        <el-col :span="6"><MetricCard label="待处理事件" :value="overview?.incidents ?? 0" /></el-col>
      </el-row>
    </div>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { usePageState } from '@/composables/usePageState'
import MetricCard from '@/components/base/MetricCard.vue'
import { fetchOverview } from '../api'
import { hasPerm } from '@/permissions'

const { state, error, startLoading, finish, fail, deny } = usePageState()
let overview = null

onMounted(async () => {
  if (!hasPerm('dashboard:read')) { deny(); return }
  startLoading()
  try { overview = await fetchOverview(); finish(overview) } catch (e) { fail(e) }
})
</script>

<style scoped>
.v2-page-title { font-size: 18px; margin: 0 0 12px; }
.v2-min-h { min-height: 120px; }
</style>