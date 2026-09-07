<template>
  <SearchTable
    title="我的数据"
    :loading="table.loading"
    :total="table.total"
    v-model:current-page="table.currentPage"
    v-model:page-size="table.pageSize"
    :show-add="false"
  >
    <template #default>
      <el-table :data="table.pagedData" v-loading="table.loading" stripe style="width: 100%">
        <el-table-column prop="resourceName" label="资源名称" min-width="150" />
        <el-table-column prop="resourceType" label="资源类型" width="120">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.resourceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="accessType" label="访问类型" width="100">
          <template #default="{ row }">
            <el-tag :type="accessTypeTag(row.accessType)" effect="light">{{ row.accessType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="160" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
      </el-table>
    </template>
  </SearchTable>
</template>

<script setup>
import { onMounted } from 'vue'
import { mydataApi } from '@/api/modules/mydata'
import { useTable } from '@/composables/useTable'
import SearchTable from '@/components/SearchTable.vue'

const table = useTable(() => mydataApi.list())

function accessTypeTag(type) {
  const map = { OWNER: '', GRANTED: 'success', FAVORITE: 'warning' }
  return map[type] || 'info'
}

onMounted(() => { table.fetch() })
</script>