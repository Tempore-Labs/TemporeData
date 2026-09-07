<template>
  <SearchTable
    title="接口日志"
    :loading="table.loading"
    :total="table.total"
    v-model:current-page="table.currentPage"
    v-model:page-size="table.pageSize"
    :show-add="false"
  >
    <template #default>
      <el-table :data="table.pagedData" v-loading="table.loading" stripe style="width: 100%">
        <el-table-column prop="apiName" label="接口名称" min-width="150" />
        <el-table-column prop="apiPath" label="路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="method" label="方法" width="80">
          <template #default="{ row }">
            <el-tag :type="methodTag(row.method)" effect="light">{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestIp" label="请求IP" width="140" />
        <el-table-column prop="responseCode" label="响应码" width="100">
          <template #default="{ row }">
            <el-tag :type="row.responseCode === 200 ? 'success' : 'danger'" effect="light">{{ row.responseCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="100" align="center" />
        <el-table-column prop="createTime" label="时间" width="160" />
      </el-table>
    </template>
  </SearchTable>
</template>

<script setup>
import { onMounted } from 'vue'
import { apilogApi } from '@/api/modules/apilog'
import { useTable } from '@/composables/useTable'
import SearchTable from '@/components/SearchTable.vue'

const table = useTable(() => apilogApi.list())

function methodTag(method) {
  const map = { GET: 'success', POST: '', PUT: 'warning', DELETE: 'danger' }
  return map[method] || 'info'
}

onMounted(() => { table.fetch() })
</script>