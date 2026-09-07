<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">日志查询</h2>
    </div>

    <el-alert v-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限查看日志" />

    <template v-else>
      <!-- Query form: real contract only, no fake data. -->
      <el-form :inline="true" class="v2-log-form" @submit.prevent="search">
        <el-form-item label="级别">
          <el-select v-model="query.level" placeholder="全部级别" clearable style="width: 130px">
            <el-option v-for="l in levels" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="query.source" placeholder="来源（服务/模块）" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="搜索日志内容" clearable style="width: 200px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" :loading="state === 'loading'" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <div v-if="searched">
        <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
        <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
        <DataTable v-else :rows="rows" :columns="columns" :state="state">
          <template #level="{ row }"><StatusBadge :value="LOG_LEVEL_TONE[row.level] || 'info'" /></template>
        </DataTable>
      </div>
      <el-empty v-else description="请设定查询条件后搜索日志（后端 /api/v1/logs 就绪后可用）" :image-size="80" />
    </template>
  </section>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { LOG_LEVEL_TONE } from '../types'
import { listLogs } from '../api'

const levels = ['ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE']
const query = reactive({ level: '', source: '', keyword: '' })
const rows = ref([])
const searched = ref(false)
const { state, error, startLoading, finish, fail, deny } = usePageState()

const columns = [
  { prop: 'timestamp', label: '时间', width: 180 },
  { slot: 'level', label: '级别', width: 110 },
  { prop: 'source', label: '来源', width: 160 },
  { prop: 'message', label: '日志内容', minWidth: 320 }
]

async function search() {
  const params = { ...query }
  ;['level', 'source', 'keyword'].forEach((k) => { if (!params[k]) delete params[k] })
  searched.value = true
  startLoading()
  try {
    rows.value = (await listLogs(params)) || []
    finish(rows.value, rows.value.length === 0)
  } catch (e) { fail(e) }
}

function reset() {
  query.level = ''; query.source = ''; query.keyword = ''
  rows.value = []; searched.value = false
}

onMounted(() => { if (!hasPerm('log:read')) { deny(); return } })
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-log-form { margin-bottom: 8px; }
.v2-min-h { min-height: 120px; }
</style>