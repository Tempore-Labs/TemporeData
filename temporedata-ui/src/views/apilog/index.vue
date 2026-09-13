<template>
  <div class="page">
    <PageHeader title="API 日志" subtitle="数据服务 API 调用日志审计" />

    <el-skeleton v-if="loading" :rows="8" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="apiName" label="接口名称" min-width="160">
        <template #default="{ row }"><span class="mono">{{ row.apiName || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="apiPath" label="路径" min-width="220" show-overflow-tooltip>
        <template #default="{ row }"><span class="mono">{{ row.apiPath || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="method" label="方法" width="90">
        <template #default="{ row }">
          <el-tag size="small" effect="plain" type="primary">{{ row.method || '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="responseCode" label="响应码" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="(row.responseCode ?? 500) < 400 ? 'success' : 'danger'">{{ row.responseCode ?? '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="requestIp" label="来源IP" width="130">
        <template #default="{ row }"><span class="mono">{{ row.requestIp || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="costTime" label="耗时(ms)" width="110" align="right">
        <template #default="{ row }"><span class="mono">{{ row.costTime ?? '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" min-width="150">
        <template #default="{ row }"><span class="mono">{{ row.createTime || '—' }}</span></template>
      </el-table-column>
    </DataTable>
    <el-pagination
      v-if="total > size"
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="onPage"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import { apilogApi, type ApilogEntity } from '@/api/ops.module'

const rows = ref<ApilogEntity[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const res = await apilogApi.page({ page: page.value - 1, size })
    rows.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}
load()
function onPage(p: number) {
  page.value = p
  load()
}
</script>

<style scoped>
.page { padding: 24px; }
.pager { margin-top: 16px; justify-content: flex-end; }
.mono { font-family: var(--td-font-mono); }
</style>