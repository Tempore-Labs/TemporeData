<template>
  <SearchTable
    title="权限审批"
    :loading="table.loading"
    :total="table.total"
    v-model:current-page="table.currentPage"
    v-model:page-size="table.pageSize"
    :show-add="false"
  >
    <template #default>
      <el-table :data="table.pagedData" v-loading="table.loading" stripe style="width: 100%">
        <el-table-column prop="applicantName" label="申请人" width="120" />
        <el-table-column prop="resourceName" label="资源名称" min-width="150" />
        <el-table-column prop="accessType" label="权限类型" width="100">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.accessType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申请原因" min-width="200" />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button size="small" type="success" link @click="handleApprove(row)">
                <el-icon><Select /></el-icon>通过
              </el-button>
              <el-button size="small" type="danger" link @click="handleReject(row)">
                <el-icon><CloseBold /></el-icon>拒绝
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </SearchTable>
</template>

<script setup>
import { onMounted } from 'vue'
import { permapprovalApi } from '@/api/modules/permapproval'
import { useTable } from '@/composables/useTable'
import { ElMessage } from 'element-plus'
import SearchTable from '@/components/SearchTable.vue'
import { Select, CloseBold } from '@element-plus/icons-vue'

const table = useTable(() => permapprovalApi.pending())

function statusTag(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

async function handleApprove(row) {
  try {
    await permapprovalApi.approve(row.id)
    ElMessage.success('已通过审批')
    await table.fetch()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

async function handleReject(row) {
  try {
    await permapprovalApi.reject(row.id)
    ElMessage.success('已拒绝审批')
    await table.fetch()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

onMounted(() => { table.fetch() })
</script>