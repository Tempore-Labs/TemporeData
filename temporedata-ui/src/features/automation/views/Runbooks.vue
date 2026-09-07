<template>
  <section class="v2-page">
    <div class="v2-page-head">
      <h2 class="v2-page-title">自动化 Runbook</h2>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div v-if="state === 'loading'" v-loading="true" class="v2-min-h" />
    <el-empty v-else-if="state === 'empty'" description="暂无 Runbook" :image-size="80" />
    <el-alert v-else-if="state === 'error'" type="error" :closable="false" :title="error?.message || '加载失败'" />
    <el-alert v-else-if="state === 'permission-denied'" type="warning" :closable="false" title="无权限查看自动化" />

    <DataTable v-else :rows="rows" :columns="columns" :state="state">
      <template #status="{ row }"><StatusBadge :value="RUNBOOK_TONE[row.status] || 'unknown'" /></template>
      <template #actions="{ row }">
        <el-button size="small" type="warning" link :disabled="!canRun" @click="openPreview(row)">执行</el-button>
      </template>
    </DataTable>

    <!-- High-risk action preview: execution is confirmed here, never run directly. -->
    <el-dialog v-model="preview.visible" title="执行 Runbook（高风险）" width="520px" :close-on-click-modal="false">
      <template v-if="preview.row">
        <el-alert type="warning" :closable="false" show-icon
          title="该操作会立即执行 Runbook，属于高风险操作，请确认后继续。" />
        <el-descriptions :column="1" border size="small" class="v2-preview-desc">
          <el-descriptions-item label="名称">{{ preview.row.name }}</el-descriptions-item>
          <el-descriptions-item label="ID">{{ preview.row.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ preview.row.status }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="preview.visible = false">取消</el-button>
        <el-button type="primary" :loading="preview.submitting" @click="run">确认执行</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { usePageState } from '@/composables/usePageState'
import { hasPerm, canExecute } from '@/permissions'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { RUNBOOK_TONE } from '../types'
import { listRunbooks, executeRunbook } from '../api'

// High-risk write: only canExecute (excludes HIGH_RISK perms) may run.
const canRun = canExecute('automation:item:execute')
const rows = ref([])
const { state, error, startLoading, finish, fail, deny } = usePageState()
const preview = ref({ visible: false, row: null, submitting: false })

const columns = [
  { prop: 'name', label: '名称', width: 180 },
  { prop: 'description', label: '描述', minWidth: 220 },
  { prop: 'triggerType', label: '触发类型', width: 120 },
  { slot: 'status', label: '状态', width: 110 },
  { prop: 'createTime', label: '创建时间', width: 170 },
  { slot: 'actions', label: '操作', width: 100, align: 'center' }
]

async function load() {
  startLoading()
  try { rows.value = (await listRunbooks()) || []; finish(rows.value, rows.value.length === 0) } catch (e) { fail(e) }
}

function openPreview(row) {
  if (!canRun) { ElMessage.warning('缺少 automation:item:execute 权限或属于高风险操作'); return }
  preview.value = { visible: true, row, submitting: false }
}

async function run() {
  preview.value.submitting = true
  try {
    await executeRunbook(preview.value.row.id)
    ElMessage.success('已触发执行')
    preview.value.visible = false
  } catch (e) { ElMessage.error(e?.message || '执行失败') } finally { preview.value.submitting = false }
}

onMounted(async () => { if (!hasPerm('automation:read')) { deny(); return } load() })
</script>

<style scoped>
.v2-page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.v2-page-title { font-size: 18px; margin: 0; }
.v2-min-h { min-height: 120px; }
.v2-preview-desc { margin-top: 12px; }
</style>