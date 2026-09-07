<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">元数据</h2>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限查看元数据" />

    <div v-else>
      <el-row :gutter="16">
        <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.key">
          <MetricCard :label="card.label" :value="card.value" />
        </el-col>
      </el-row>
      <p class="v2-meta-note">
        元数据采集由 /api/public/meta 内部驱动（zy_meta_table / zy_meta_column）。完整采集状态与表字段详情见旧版「元数据管理」页。
      </p>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'
import MetricCard from '@/components/base/MetricCard.vue'
import { listMetaOverview } from '../api'

const { state, error, startLoading, finish, fail, deny } = usePageState()
const overview = ref({})

const statCards = computed(() => [
  { key: 'tables', label: '已采集表', value: overview.value.totalTables ?? '-' },
  { key: 'columns', label: '字段总数', value: overview.value.totalColumns ?? '-' },
  { key: 'collected', label: '已采集数据源', value: overview.value.collectedCount ?? '-' },
  { key: 'datasources', label: '数据源总数', value: overview.value.datasourceCount ?? '-' }
])

async function load() {
  startLoading()
  try { overview.value = (await listMetaOverview()) || {}; finish(overview.value) } catch (e) { fail(e) }
}

onMounted(async () => { if (!hasPerm('data:read')) { deny(); return } load() })
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-min-h { min-height: 120px; }
.v2-meta-note { margin-top: 16px; color: #94a3b8; font-size: 13px; }
</style>