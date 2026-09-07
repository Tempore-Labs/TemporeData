<template>
  <div class="audit-log-page">
    <div class="page-header">
      <h2><el-icon class="header-icon"><Tickets /></el-icon>审计日志</h2>
      <p>TemporeData 大数据平台 · 操作审计 / 登录日志 / 行为日志统一查询</p>
    </div>

    <!-- 类型切换 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-bar">
        <el-radio-group v-model="logType" @change="handleTypeChange">
          <el-radio-button label="AUDIT">操作审计</el-radio-button>
          <el-radio-button label="LOGIN">登录日志</el-radio-button>
          <el-radio-button label="BEHAVIOR">行为日志</el-radio-button>
        </el-radio-group>

        <div v-if="logType === 'AUDIT'" class="filter-item">
          <span class="filter-label">操作类型</span>
          <el-select v-model="filters.operation" placeholder="全部类型" clearable style="width: 140px">
            <el-option label="创建" value="CREATE" />
            <el-option label="更新" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="查询" value="QUERY" />
            <el-option label="登录" value="LOGIN" />
          </el-select>
        </div>
        <div class="filter-item">
          <span class="filter-label">用户名</span>
          <el-input v-model="filters.username" placeholder="请输入用户名" clearable style="width: 160px" />
        </div>
        <div class="filter-actions">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- 日志表格 -->
    <el-card class="table-card" shadow="never">
      <!-- 操作审计 -->
      <el-table v-if="logType === 'AUDIT'" :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="operator" label="操作用户" width="130" />
        <el-table-column label="操作类型" width="110">
          <template #default="{ row }">
            <el-tag
              :type="operationTagType(row.action)"
              :class="{ 'tag-purple': row.action === 'LOGIN' }"
              effect="light"
              size="small"
            >
              {{ operationLabel(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作资源" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.resourceKey || row.resourceType || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作结果" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" effect="dark" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" width="150" />
        <el-table-column prop="eventTime" label="操作时间" width="180" />
      </el-table>

      <!-- 登录日志 -->
      <el-table v-else-if="logType === 'LOGIN'" :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="operator" label="用户名" width="130" />
        <el-table-column label="登录方式" width="110">
          <template #default="{ row }">
            <el-tag effect="light" size="small">{{ loginTypeLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" effect="light" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resourceKey" label="失败原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="eventTime" label="时间" width="180" />
      </el-table>

      <!-- 行为日志 -->
      <el-table v-else :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="operator" label="用户名" width="130" />
        <el-table-column prop="action" label="操作" min-width="140" show-overflow-tooltip />
        <el-table-column prop="resourceType" label="目标类型" width="120" />
        <el-table-column prop="resourceKey" label="目标名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="150" />
        <el-table-column prop="eventTime" label="时间" width="180" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[15, 30, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="fetchLogs"
          @size-change="fetchLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Tickets, Search, Refresh } from '@element-plus/icons-vue'
import { auditApi } from '@/api/modules/audit'

const loading = ref(false)
const tableData = ref([])
const logType = ref('AUDIT')

const filters = reactive({
  operation: '',
  username: '',
  dateRange: null
})

const pagination = reactive({
  page: 1,
  size: 15,
  total: 0
})

function operationTagType(operation) {
  const map = {
    CREATE: '',
    UPDATE: 'warning',
    DELETE: 'danger',
    QUERY: 'success',
    LOGIN: 'info'
  }
  return map[operation] || 'info'
}

function operationLabel(operation) {
  const map = {
    CREATE: '创建',
    UPDATE: '更新',
    DELETE: '删除',
    QUERY: '查询',
    LOGIN: '登录'
  }
  return map[operation] || operation
}

function loginTypeLabel(t) {
  const map = { PASSWORD: '密码', SMS: '短信', EMAIL: '邮箱', OAUTH: '第三方', LDAP: 'LDAP' }
  return map[t] || t || '-'
}

async function fetchLogs() {
  loading.value = true
  try {
    // All three tabs read from the unified audit endpoint (type filter server-side).
    const eventType = logType.value === 'AUDIT' ? 'AUDIT' : logType.value
    const action = eventType === 'AUDIT' ? (filters.operation || undefined) : undefined
    const operator = eventType === 'AUDIT' ? (filters.username || undefined) : undefined
    const data = await auditApi.eventsPage(eventType, operator, action, pagination.page - 1, pagination.size)
    tableData.value = data?.content || []
    pagination.total = data?.totalElements || 0
  } catch (e) {
    console.error('Failed to fetch logs:', e)
  } finally {
    loading.value = false
  }
}

function handleTypeChange() {
  pagination.page = 1
  fetchLogs()
}

function handleSearch() {
  pagination.page = 1
  fetchLogs()
}

function handleReset() {
  filters.operation = ''
  filters.username = ''
  filters.dateRange = null
  pagination.page = 1
  fetchLogs()
}

onMounted(() => {
  fetchLogs()
})
</script>

<style scoped>
.audit-log-page {
  padding: 0;
}

.header-icon {
  margin-right: 6px;
  vertical-align: middle;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: var(--spark-radius-md);
  box-shadow: var(--spark-shadow);
}

.filter-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  color: var(--spark-text-muted);
  white-space: nowrap;
}

.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.table-card {
  border-radius: var(--spark-radius-md);
  box-shadow: var(--spark-shadow);
}

.table-card :deep(.el-card__body) {
  padding: 12px 20px 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
}

.tag-purple {
  --el-tag-bg-color: #f3f0ff;
  --el-tag-border-color: #c4b5fd;
  --el-tag-text-color: #7c3aed;
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-actions {
    margin-left: 0;
  }
}
</style>