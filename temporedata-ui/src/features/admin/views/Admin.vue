<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">管理</h2>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限访问管理" />

    <div v-else class="v2-admin">
      <el-row :gutter="16" class="v2-admin-cards">
        <el-col :xs="12" :sm="6"><MetricCard label="租户数" :value="tenants.length || 0" /></el-col>
        <el-col :xs="12" :sm="6"><MetricCard label="组织节点" :value="orgCount" accent /></el-col>
      </el-row>

      <el-card class="section-card" shadow="never">
        <template #header><span class="section-title">租户</span></template>
        <DataTable :rows="tenants" :columns="tenantColumns" :state="state">
          <template #status="{ row }"><StatusBadge :value="ADMIN_TONE[row.status] || 'unknown'" /></template>
        </DataTable>
      </el-card>

      <el-card class="section-card" shadow="never">
        <template #header><span class="section-title">组织</span></template>
        <div v-if="orgTree.length">
          <el-tree :data="orgTree" :props="orgProps" />
        </div>
        <el-empty v-else description="暂无组织数据" :image-size="60" />
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import MetricCard from '@/components/base/MetricCard.vue'
import { ADMIN_TONE } from '../types'
import { listTenants, listOrgTree } from '../api'

const tenants = ref([])
const orgTree = ref([])
const { state, error, startLoading, finish, fail, deny } = usePageState()

const tenantColumns = [
  { prop: 'name', label: '租户名称', width: 200 },
  { slot: 'status', label: '状态', width: 120 },
  { prop: 'createTime', label: '创建时间', width: 180 }
]

const orgCount = computed(() => {
  const count = (nodes) => (nodes || []).reduce((n, node) => n + 1 + count(node.children), 0)
  return count(orgTree.value)
})

async function load() {
  startLoading()
  try {
    const settled = await Promise.allSettled([listTenants(), listOrgTree()])
    tenants.value = settled[0].status === 'fulfilled' ? (settled[0].value || []) : []
    orgTree.value = settled[1].status === 'fulfilled' ? (settled[1].value || []) : []
    finish(tenants.value, tenants.value.length === 0 && orgTree.value.length === 0)
  } catch (e) { fail(e) }
}

onMounted(async () => { if (!hasPerm('admin:read')) { deny(); return } load() })
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-min-h { min-height: 120px; }
.section-card { margin-top: 16px; }
.section-title { font-size: 15px; font-weight: 600; }
</style>