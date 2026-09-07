<template>
  <div class="change-audit-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">变更审计</h2>
        <div class="page-desc">资产级操作审计 · 记录标签绑定、术语表、指标执行、元数据备注等关键变更</div>
      </div>
    </div>

    <el-card shadow="never">
      <!-- Filters -->
      <div class="filter-bar">
        <el-select v-model="filters.entityType" placeholder="实体类型" clearable style="width: 150px">
          <el-option v-for="t in entityTypes" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-input v-model="filters.entityId" placeholder="实体ID" clearable style="width: 200px" />
        <el-select v-model="filters.action" placeholder="操作" clearable style="width: 130px">
          <el-option v-for="a in actions" :key="a" :label="a" :value="a" />
        </el-select>
        <el-input v-model="filters.operator" placeholder="操作人" clearable style="width: 150px" />
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 360px"
        />
        <el-button type="primary" :icon="Search" @click="load(0)">查询</el-button>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>

      <el-table :data="records" stripe v-loading="loading">
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="entityType" label="实体类型" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="entityTypeTag(row.entityType)">{{ row.entityType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="entityName" label="实体" min-width="160" show-overflow-tooltip />
        <el-table-column prop="action" label="操作" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="actionTag(row.action)">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="110">
          <template #default="{ row }">{{ row.operator || '-' }}</template>
        </el-table-column>
        <el-table-column label="变更内容" min-width="260">
          <template #default="{ row }">
            <div v-if="row.beforeValue || row.afterValue" class="diff-cell">
              <span v-if="row.beforeValue" class="diff-before">{{ row.beforeValue }}</span>
              <el-icon v-if="row.beforeValue && row.afterValue" class="diff-arrow"><Right /></el-icon>
              <span v-if="row.afterValue" class="diff-after">{{ row.afterValue }}</span>
            </div>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="entityId" label="实体ID" width="200" show-overflow-tooltip />
      </el-table>

      <div class="pagination-row">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page + 1"
          @current-change="onPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, Refresh, Right } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { auditApi } from '@/api/modules/audit'

const entityTypes = [
  { value: 'TABLE', label: '表' },
  { value: 'COLUMN', label: '字段' },
  { value: 'INDICATOR', label: '指标' },
  { value: 'TAG', label: '标签' },
  { value: 'TAG_BINDING', label: '标签绑定' },
  { value: 'CLASSIFICATION', label: '标签分类' },
  { value: 'GLOSSARY', label: '术语表' },
  { value: 'GLOSSARY_TERM', label: '术语' }
]

const actions = ['CREATE', 'UPDATE', 'DELETE', 'BIND', 'UNBIND', 'EXECUTE', 'APPROVED', 'REJECTED', 'SUBMIT', 'IN_REVIEW']

const filters = ref({ entityType: '', entityId: '', action: '', operator: '' })
const dateRange = ref(null)
const records = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(0)
const size = ref(20)

onMounted(() => load(0))

async function load(targetPage) {
  loading.value = true
  try {
    const operator = filters.value.operator || undefined
    const action = filters.value.action || undefined
    const resourceType = filters.value.entityType || undefined
    const res = await auditApi.eventsPage('CHANGE', operator, action, targetPage, size.value, resourceType)
    const content = res?.content || []
    records.value = content.map(r => ({
      ...r,
      createTime: r.eventTime,
      entityType: r.resourceType,
      entityName: r.resourceKey || '-',
      entityId: r.resourceKey,
      beforeValue: r.detailJson ? extractJson(r.detailJson, 'before') : '',
      afterValue: r.detailJson ? extractJson(r.detailJson, 'after') : ''
    }))
    total.value = res?.totalElements || 0
    page.value = targetPage
  } catch (e) {
    ElMessage.error('查询失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function extractJson(json, key) {
  try {
    const o = JSON.parse(json)
    return o[key] ?? ''
  } catch (e) {
    return ''
  }
}

function onPageChange(p) {
  load(p - 1)
}

function resetFilters() {
  filters.value = { entityType: '', entityId: '', action: '', operator: '' }
  dateRange.value = null
  load(0)
}

function entityTypeTag(type) {
  return { TABLE: 'success', COLUMN: 'info', INDICATOR: 'warning', TAG: 'danger', GLOSSARY: 'primary' }[type] || 'info'
}

function actionTag(action) {
  if (action === 'DELETE' || action === 'REJECTED') return 'danger'
  if (action === 'EXECUTE' || action === 'BIND' || action === 'UNBIND') return 'warning'
  if (action === 'APPROVED') return 'success'
  return 'info'
}
</script>

<style scoped>
.change-audit-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.page-desc {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
}

.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  align-items: center;
}

.diff-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  flex-wrap: wrap;
}

.diff-before {
  color: #dc2626;
  text-decoration: line-through;
}

.diff-after {
  color: #16a34a;
}

.diff-arrow {
  color: #9ca3af;
  font-size: 12px;
}

.muted {
  color: #c0c4cc;
}

.pagination-row {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>