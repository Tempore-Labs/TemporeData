<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">数据源</h2>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-empty v-else-if="state === 'empty'" description="暂无数据源" :image-size="80" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限查看数据源" />

    <DataTable v-else :rows="rows" :columns="columns" :state="state">
      <template #status="{ row }"><StatusBadge :value="DATASOURCE_TONE[row.status] || 'unknown'" /></template>
      <template #actions="{ row }">
        <el-button size="small" type="warning" link :loading="testingId === row.id" @click="handleTest(row)">测试</el-button>
      </template>
    </DataTable>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { usePageState } from '@/composables/usePageState'
import { hasPerm } from '@/permissions'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { DATASOURCE_TONE } from '../types'
import { listDatasources, testDatasource } from '../api'

const rows = ref([])
const testingId = ref('')
const { state, error, startLoading, finish, fail, deny } = usePageState()

const columns = [
  { prop: 'name', label: '数据源名称', width: 160 },
  { prop: 'type', label: '类型', width: 130 },
  { prop: 'host', label: '主机地址', width: 180 },
  { prop: 'port', label: '端口', width: 90, align: 'center' },
  { slot: 'status', label: '状态', width: 120 },
  { slot: 'actions', label: '操作', width: 100, align: 'center' }
]

async function load() {
  startLoading()
  try { rows.value = (await listDatasources()) || []; finish(rows.value, rows.value.length === 0) } catch (e) { fail(e) }
}

async function handleTest(row) {
  if (!hasPerm('data:write')) { ElMessage.warning('缺少 data:write 权限'); return }
  testingId.value = row.id
  try { await testDatasource(row.id); ElMessage.success('连接成功') } catch (e) { ElMessage.error(e?.message || '连接失败') } finally { testingId.value = '' }
}

onMounted(async () => { if (!hasPerm('data:read')) { deny(); return } load() })
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-min-h { min-height: 120px; }
</style>