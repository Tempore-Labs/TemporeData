<template>
  <div class="audit-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">审计中心</h2>
        <p class="page-desc">统一审计事件（哈希链防篡改）+ 完整性校验 + 合规导出 + 归档</p>
      </div>
      <div class="header-actions">
        <el-button :icon="VideoPlay" @click="runVerify" :loading="verifying">完整性校验</el-button>
        <el-button :icon="Download" @click="doExport">导出 CSV</el-button>
        <el-button type="primary" :icon="Tickets" @click="doArchive">归档</el-button>
      </div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="审计事件" name="events" />
      <el-tab-pane label="审计策略" name="policies" />
      <el-tab-pane label="归档" name="archives" />
    </el-tabs>

    <el-card v-if="tab === 'events'" shadow="never">
      <div class="toolbar">
        <el-input v-model="moduleFilter" placeholder="模块" clearable style="width: 160px" />
        <el-input v-model="operatorFilter" placeholder="操作者" clearable style="width: 160px" />
        <el-button :icon="Search" @click="loadEvents">查询</el-button>
        <el-alert v-if="verifyResult" :type="verifyResult.ok ? 'success' : 'error'" :closable="false" show-icon
                  :title="verifyResult.ok ? `链路完整，共校验 ${verifyResult.checked} 条` : `检测到 ${verifyResult.brokenIds.length} 条被篡改`"
                  style="flex:1" />
      </div>
      <el-table :data="events" v-loading="loading" stripe>
        <el-table-column prop="seq" label="SEQ" width="70" />
        <el-table-column prop="eventTime" label="时间" min-width="150" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="动作" width="120" />
        <el-table-column prop="operator" label="操作者" width="110" />
        <el-table-column prop="resourceType" label="资源类型" width="100" />
        <el-table-column prop="resourceKey" label="资源Key" min-width="130" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column label="哈希" width="90">
          <template #default="{ row }">
            <el-tooltip :content="row.eventHash" placement="top"><span class="hash">••••</span></el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="tab === 'policies'" shadow="never">
      <el-table :data="policies" v-loading="loading" stripe>
        <el-table-column prop="module" label="模块" min-width="120" />
        <el-table-column prop="action" label="动作" min-width="150">
          <template #default="{ row }">{{ row.action || '（全部）' }}</template>
        </el-table-column>
        <el-table-column prop="auditLevel" label="级别" width="110">
          <template #default="{ row }"><el-tag size="small">{{ row.auditLevel }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="retentionDays" label="保留天数" width="110" />
        <el-table-column label="告警" width="90">
          <template #default="{ row }">{{ row.notifyAlert ? '开' : '关' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="tab === 'archives'" shadow="never">
      <el-table :data="archives" v-loading="loading" stripe>
        <el-table-column prop="periodStart" label="起始" min-width="150" />
        <el-table-column prop="periodEnd" label="结束" min-width="150" />
        <el-table-column prop="recordCount" label="记录数" width="100" />
        <el-table-column prop="rootHash" label="RootHash" min-width="200" show-overflow-tooltip />
        <el-table-column prop="signedBy" label="签名" width="120" />
        <el-table-column prop="createTime" label="归档时间" min-width="150" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { VideoPlay, Download, Tickets, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { auditApi } from '@/api/modules/audit'

const tab = ref('events')
const loading = ref(false)
const verifying = ref(false)
const events = ref([])
const policies = ref([])
const archives = ref([])
const verifyResult = ref(null)
const moduleFilter = ref('')
const operatorFilter = ref('')

async function loadEvents() {
  loading.value = true
  try {
    events.value = await auditApi.events(moduleFilter.value || undefined, operatorFilter.value || undefined) || []
  } catch (e) { ElMessage.error('加载审计事件失败') } finally { loading.value = false }
}

async function loadAll() {
  loading.value = true
  try {
    const [e, p, a] = await Promise.all([auditApi.events(), auditApi.policies(), auditApi.archives()])
    events.value = e || []
    policies.value = p || []
    archives.value = a || []
  } catch (err) { ElMessage.error('加载失败') } finally { loading.value = false }
}

async function runVerify() {
  verifying.value = true
  try {
    verifyResult.value = await auditApi.verify()
    ElMessage.success(verifyResult.value.ok ? '审计链路完整' : '发现篡改！')
  } catch (e) {
    ElMessage.error('校验失败')
  } finally { verifying.value = false }
}

async function doExport() {
  try {
    const res = await auditApi.export()
    const blob = new Blob([res.csv || ''], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `audit_export_${Date.now()}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success(`已导出 ${res.records} 条；RootHash=${res.rootHash.slice(0, 12)}…`)
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

async function doArchive() {
  try {
    const ar = await auditApi.archive()
    ElMessage.success(`已归档 ${ar.recordCount} 条`)
    await loadAll()
  } catch (e) {
    ElMessage.error('归档失败')
  }
}

onMounted(loadAll)
</script>

<style scoped>
.audit-page { max-width: 1280px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.page-desc { font-size: 13px; color: #9ca3af; margin: 4px 0 0; }
.header-actions { display: flex; gap: 8px; }
.toolbar { display: flex; gap: 8px; margin-bottom: 12px; align-items: center; }
.hash { letter-spacing: 2px; color: #6b7280; }
</style>