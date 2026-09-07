<template>
  <SearchTable
    title="消息中心"
    :loading="loading"
    :total="total"
    v-model:current-page="currentPage"
    v-model:page-size="pageSize"
    :show-add="false"
  >
    <template #default>
      <el-table :data="pagedData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已读" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isRead === 1 ? 'success' : 'warning'" effect="light">
              {{ row.isRead === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.isRead !== 1"
              size="small"
              type="primary"
              link
              @click="handleMarkRead(row)"
            >
              标记已读
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </SearchTable>
</template>

<script setup>
import { onMounted } from 'vue'
import { messageApi } from '@/api/modules/message'
import { useTable } from '@/composables/useTable'
import { ElMessage } from 'element-plus'
import SearchTable from '@/components/SearchTable.vue'

function getUserId() {
  try {
    const user = JSON.parse(localStorage.getItem('td_user') || '{}')
    return user.userId || '866103203da44dfaa53f9098826a334a'
  } catch {
    return '866103203da44dfaa53f9098826a334a'
  }
}

// Destructure useTable's top-level refs so the template auto-unwraps them
// (nested `table.xxx` refs are NOT unwrapped and would leak a Ref into el-table).
const {
  data,
  loading,
  total,
  currentPage,
  pageSize,
  pagedData,
  fetch: fetchData
} = useTable(() => messageApi.list(getUserId()))

async function handleMarkRead(row) {
  try {
    await messageApi.markRead(row.id)
    ElMessage.success('已标记为已读')
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

onMounted(() => fetchData())
</script>