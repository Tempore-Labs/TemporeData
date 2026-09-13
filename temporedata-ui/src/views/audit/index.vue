<template>
  <div class="page">
    <PageHeader title="审计中心" subtitle="统一审计事件（哈希链防篡改）+ 归档 + 审计策略">
      <template #actions>
        <el-button size="small" :loading="verifying" @click="verify">链校验</el-button>
        <el-button size="small" :loading="archiving" @click="doArchive">归档</el-button>
        <el-button size="small" type="primary" @click="doExport">导出</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- 审计事件 -->
      <el-tab-pane label="审计事件" name="events">
        <div class="filter-bar">
          <el-input v-model="kw" placeholder="操作人 / 模块 / 动作" size="small" style="width: 240px" clearable @keyup.enter="loadEvents" />
          <el-button size="small" @click="loadEvents">查询</el-button>
        </div>
        <el-skeleton v-if="eventsLoading" :rows="6" animated />
        <DataTable v-else :data="events">
          <el-table-column prop="eventTime" label="时间" min-width="160">
            <template #default="{ row }"><span class="mono">{{ row.eventTime || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="operator" label="操作人" width="110" />
          <el-table-column prop="action" label="动作" width="110">
            <template #default="{ row }"><el-tag size="small">{{ row.action || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="resourceType" label="资源类型" width="120" />
          <el-table-column prop="resourceKey" label="资源" min-width="160">
            <template #default="{ row }"><span class="mono">{{ row.resourceKey || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="module" label="模块" width="110" />
          <el-table-column prop="seq" label="序号" width="80">
            <template #default="{ row }"><span class="mono">{{ row.seq ?? '—' }}</span></template>
          </el-table-column>
          <el-table-column label="链哈希" width="90">
            <template #default="{ row }">
              <el-tooltip :content="row.eventHash || ''" placement="top">
                <span class="mono hash">{{ row.eventHash ? (row.eventHash.slice(0, 8) + '…') : '—' }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
        </DataTable>
        <el-pagination
          v-if="total > size"
          class="pager"
          layout="total, prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="onPage"
        />
      </el-tab-pane>

      <!-- 聚合校验 -->
      <el-tab-pane label="聚合校验" name="verify">
        <div v-if="verifyResult" class="verify-panel">
          <el-alert
            :type="verifyResult.valid ? 'success' : 'danger'"
            :title="verifyResult.valid ? '链完整：审计事件未被篡改' : '链校验失败：存在篡改'"
            :closable="false"
            show-icon
          />
          <el-descriptions :column="3" border size="small" class="verify-meta" v-if="Object.keys(verifyResult).length">
            <el-descriptions-item v-for="(v, k) in verifyResult" :key="k" :label="k">
              <span class="mono">{{ String(v) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <div v-else class="verify-empty">
          点击顶部「链校验」验证审计哈希链完整性与防篡改性
        </div>
      </el-tab-pane>

      <!-- 归档 -->
      <el-tab-pane label="审计归档" name="archives">
        <el-skeleton v-if="archiveLoading" :rows="5" animated />
        <DataTable v-else :data="archives">
          <el-table-column prop="periodStart" label="开始" min-width="140">
            <template #default="{ row }"><span class="mono">{{ row.periodStart || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="periodEnd" label="结束" min-width="140">
            <template #default="{ row }"><span class="mono">{{ row.periodEnd || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="recordCount" label="记录数" width="100">
            <template #default="{ row }"><span class="mono">{{ row.recordCount ?? '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="rootHash" label="根哈希" min-width="220">
            <template #default="{ row }"><span class="mono hash">{{ row.rootHash || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="signedBy" label="签名" width="130">
            <template #default="{ row }">{{ row.signedBy || '—' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="归档时间" min-width="150">
            <template #default="{ row }"><span class="mono muted">{{ row.createTime || '—' }}</span></template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>

      <!-- 审计策略 -->
      <el-tab-pane label="审计策略" name="policies">
        <div class="filter-bar">
          <el-button size="small" type="primary" @click="openPolicy">+ 新增策略</el-button>
        </div>
        <el-skeleton v-if="policyLoading" :rows="5" animated />
        <DataTable v-else :data="policies">
          <el-table-column prop="module" label="模块" width="150" />
          <el-table-column prop="action" label="动作" width="150" />
          <el-table-column prop="auditLevel" label="审计级别" width="130">
            <template #default="{ row }"><el-tag size="small" :type="levelTag(row.auditLevel)">{{ row.auditLevel || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="retentionDays" label="保留天数" width="110">
            <template #default="{ row }"><span class="mono">{{ row.retentionDays ?? '—' }}</span></template>
          </el-table-column>
          <el-table-column label="告警" width="90">
            <template #default="{ row }">
              <StatusBadge :status="row.notifyAlert === false ? 'closed' : 'success'" :text="row.notifyAlert === false ? '否' : '是'" />
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>
    </el-tabs>

    <!-- Policy dialog -->
    <el-dialog v-model="policyVisible" title="新增审计策略" width="480px">
      <el-form :model="policyForm" label-width="90px">
        <el-form-item label="模块" required>
          <el-input v-model="policyForm.module" />
        </el-form-item>
        <el-form-item label="动作" required>
          <el-input v-model="policyForm.action" />
        </el-form-item>
        <el-form-item label="审计级别">
          <el-select v-model="policyForm.auditLevel" style="width: 100%">
            <el-option v-for="l in ['NONE', 'SUMMARY', 'FULL']" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="保留天数">
          <el-input-number v-model="policyForm.retentionDays" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="告警">
          <el-switch v-model="policyForm.notifyAlert" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="policyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePolicy">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { auditApi, type AuditArchiveEntity, type AuditEventEntity, type AuditPolicyEntity } from '@/api/audit'

const queryClient = useQueryClient()
const tab = ref('events')
const kw = ref('')

// events
const events = ref<AuditEventEntity[]>([])
const eventsLoading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)

// verify
const verifying = ref(false)
const verifyResult = ref<Record<string, unknown> | null>(null)

// archives
const archives = ref<AuditArchiveEntity[]>([])
const archiveLoading = ref(false)
const archiving = ref(false)

// policies
const policies = ref<AuditPolicyEntity[]>([])
const policyLoading = ref(false)
const policyVisible = ref(false)
const saving = ref(false)
const policyForm = ref<Partial<AuditPolicyEntity>>({})

async function loadEvents() {
  eventsLoading.value = true
  try {
    const res = await auditApi.eventsPage({ page: page.value - 1, size })
    let list = res.content || []
    if (kw.value) {
      const q = kw.value.toLowerCase()
      list = list.filter((e) =>
        (e.operator || '').toLowerCase().includes(q) ||
        (e.module || '').toLowerCase().includes(q) ||
        (e.action || '').toLowerCase().includes(q),
      )
      total.value = list.length
      events.value = list.slice(0, size)
    } else {
      total.value = res.totalElements || 0
      events.value = list
    }
  } finally {
    eventsLoading.value = false
  }
}
loadEvents()

function onPage(p: number) {
  page.value = p
  loadEvents()
}
function onTabChange(name: string | number) {
  if (name === 'archives') loadArchives()
  else if (name === 'policies') loadPolicies()
}

async function loadArchives() {
  archiveLoading.value = true
  try {
    archives.value = await auditApi.archives()
  } finally {
    archiveLoading.value = false
  }
}
async function loadPolicies() {
  policyLoading.value = true
  try {
    policies.value = await auditApi.policies()
  } finally {
    policyLoading.value = false
  }
}

async function verify() {
  verifying.value = true
  try {
    verifyResult.value = await auditApi.verifyRange(undefined, undefined)
  } finally {
    verifying.value = false
  }
}

async function doArchive() {
  archiving.value = true
  try {
    await auditApi.archive()
    ElMessage.success('归档完成')
    await loadArchives()
  } finally {
    archiving.value = false
  }
}

function doExport() {
  ElMessage.info('审计导出将在后续版本提供文件下载')
}

function openPolicy() {
  policyForm.value = { module: '', action: '', auditLevel: 'FULL', retentionDays: 90, notifyAlert: true }
  policyVisible.value = true
}
async function savePolicy() {
  if (!policyForm.value.module || !policyForm.value.action) {
    ElMessage.warning('模块与动作必填')
    return
  }
  saving.value = true
  try {
    await auditApi.savePolicy(policyForm.value)
    ElMessage.success('策略已保存')
    policyVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['audit'] })
    await loadPolicies()
  } finally {
    saving.value = false
  }
}

function levelTag(l?: string) {
  if (l === 'FULL') return 'danger'
  if (l === 'SUMMARY') return 'warning'
  return 'info'
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.mono {
  font-family: var(--td-font-mono);
}
.muted {
  color: var(--td-text-4);
}
.hash {
  color: var(--td-purple);
}
.verify-panel {
  padding: 4px 0;
}
.verify-meta {
  margin-top: 16px;
}
.verify-empty {
  padding: 48px;
  text-align: center;
  color: var(--td-text-4);
  font-size: 13px;
}
</style>