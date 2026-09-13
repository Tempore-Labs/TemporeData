<template>
  <div class="page">
    <PageHeader title="安全治理" subtitle="供应链漏洞跟踪、发布门禁与 QA 报告" />

    <!-- Gate banner -->
    <div v-if="gate" class="gate-banner" :class="gate.gate.toLowerCase()">
      <div class="gate-icon"><el-icon :size="22"><CircleCheck v-if="gate.gate === 'PASS'" /><Warning v-else /></el-icon></div>
      <div class="gate-body">
        <div class="gate-title">发布门禁: {{ gate.gate }}</div>
        <div class="gate-sub">Critical {{ gate.criticalOpen }} 个 · High {{ gate.highOpen }} 个未关闭</div>
      </div>
    </div>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- 漏洞跟踪 -->
      <el-tab-pane label="漏洞跟踪" name="vulns">
        <div class="filter-bar">
          <el-button size="small" type="primary" @click="openVuln">+ 登记漏洞</el-button>
        </div>
        <el-skeleton v-if="vulnLoading" :rows="6" animated />
        <DataTable v-else :data="vulns">
          <el-table-column prop="cve" label="CVE" min-width="150">
            <template #default="{ row }"><span class="mono">{{ row.cve || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="packageName" label="依赖包" min-width="150">
            <template #default="{ row }"><span class="mono">{{ row.packageName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="cvss" label="CVSS" width="90">
            <template #default="{ row }"><span class="mono" :style="{ color: cvssColor(row.cvss) }">{{ row.cvss ?? '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="severity" label="级别" width="110">
            <template #default="{ row }"><el-tag size="small" :type="sevTag(row.severity)">{{ row.severity || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="affectedVer" label="受影响版本" width="130">
            <template #default="{ row }"><span class="mono">{{ row.affectedVer || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="fixedVer" label="修复版本" width="120">
            <template #default="{ row }"><span class="mono">{{ row.fixedVer || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="source" label="来源" width="90">
            <template #default="{ row }"><el-tag size="small" type="info">{{ row.source || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-select v-model="row.status" size="small" style="width: 120px" @change="(v: string) => changeStatus(row, v)">
                <el-option v-for="s in ['OPEN', 'FIXING', 'FIXED', 'RISK_ACCEPTED']" :key="s" :label="s" :value="s" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="success" @click="acceptVuln(row)">豁免</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>

      <!-- SBOM -->
      <el-tab-pane label="SBOM" name="sbom">
        <div class="sbom-box">
          <pre class="sbom-json">{{ sbomText }}</pre>
        </div>
      </el-tab-pane>

      <!-- QA 报告 -->
      <el-tab-pane label="QA 报告" name="qa">
        <div class="filter-bar">
          <el-button size="small" type="primary" @click="openQa">+ 新增报告</el-button>
        </div>
        <el-skeleton v-if="qaLoading" :rows="5" animated />
        <DataTable v-else :data="qaRows">
          <el-table-column prop="version" label="版本" width="110">
            <template #default="{ row }"><span class="mono">{{ row.version || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="module" label="模块" width="140" />
          <el-table-column prop="kind" label="类型" width="130">
            <template #default="{ row }"><el-tag size="small">{{ row.kind || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="summary" label="摘要" min-width="240" show-overflow-tooltip />
          <el-table-column prop="coverage" label="覆盖率" width="100">
            <template #default="{ row }"><span class="mono">{{ row.coverage != null ? row.coverage + '%' : '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="gate" label="门禁" width="100">
            <template #default="{ row }"><StatusBadge :status="row.gate" /></template>
          </el-table-column>
          <el-table-column prop="runTime" label="运行时间" min-width="150">
            <template #default="{ row }"><span class="mono muted">{{ row.runTime || '—' }}</span></template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>
    </el-tabs>

    <!-- Vuln dialog -->
    <el-dialog v-model="vulnVisible" title="登记漏洞" width="520px">
      <el-form :model="vulnForm" label-width="100px">
        <el-form-item label="CVE" required>
          <el-input v-model="vulnForm.cve" placeholder="如 CVE-2024-XXXX" />
        </el-form-item>
        <el-form-item label="依赖包">
          <el-input v-model="vulnForm.packageName" />
        </el-form-item>
        <el-form-item label="CVSS 分数">
          <el-input-number v-model="vulnForm.cvss" :min="0" :max="10" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="vulnForm.severity" style="width: 100%">
            <el-option v-for="s in ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="受影响版本">
          <el-input v-model="vulnForm.affectedVer" />
        </el-form-item>
        <el-form-item label="修复版本">
          <el-input v-model="vulnForm.fixedVer" />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="vulnForm.source" style="width: 100%">
            <el-option v-for="s in ['SCA', 'SAST', 'MANUAL']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vulnVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveVuln">登记</el-button>
      </template>
    </el-dialog>

    <!-- QA dialog -->
    <el-dialog v-model="qaVisible" title="新增 QA 报告" width="520px">
      <el-form :model="qaForm" label-width="90px">
        <el-form-item label="版本" required>
          <el-input v-model="qaForm.version" />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="qaForm.module" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="qaForm.kind" style="width: 100%">
            <el-option v-for="k in ['UNIT', 'INTEGRATION', 'API', 'COVERAGE', 'E2E']" :key="k" :label="k" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="qaForm.summary" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="覆盖率">
          <el-input-number v-model="qaForm.coverage" :min="0" :max="100" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="门禁">
          <el-select v-model="qaForm.gate" style="width: 100%">
            <el-option v-for="g in ['PASS', 'FAIL', 'BLOCK']" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="qaVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveQa">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { securityApi, type GateCheck, type QaReportEntity, type VulnEntity } from '@/api/security'

const queryClient = useQueryClient()
const tab = ref('vulns')

// gate
const gate = ref<GateCheck | null>(null)

// vulns
const vulns = ref<VulnEntity[]>([])
const vulnLoading = ref(false)

// sbom
const sbomText = ref('点击上方「SBOM」标签加载物料清单')

// qa
const qaRows = ref<QaReportEntity[]>([])
const qaLoading = ref(false)

const vulnVisible = ref(false)
const qaVisible = ref(false)
const saving = ref(false)
const vulnForm = ref<Partial<VulnEntity>>({})
const qaForm = ref<Partial<QaReportEntity>>({})

async function loadGate() {
  try {
    gate.value = await securityApi.gate()
  } catch {
    /* optional */
  }
}
async function loadVulns() {
  vulnLoading.value = true
  try {
    vulns.value = await securityApi.vulns()
  } finally {
    vulnLoading.value = false
  }
}
async function loadSbom() {
  try {
    const sbom = await securityApi.sbom()
    sbomText.value = JSON.stringify(sbom, null, 2)
  } catch {
    sbomText.value = 'SBOM 加载失败'
  }
}
async function loadQa() {
  qaLoading.value = true
  try {
    qaRows.value = await securityApi.qa()
  } finally {
    qaLoading.value = false
  }
}

loadGate()
loadVulns()

function onTabChange(name: string | number) {
  if (name === 'sbom') loadSbom()
  else if (name === 'qa') loadQa()
}

function openVuln() {
  vulnForm.value = { cve: '', packageName: '', cvss: 5, severity: 'MEDIUM', affectedVer: '', fixedVer: '', source: 'MANUAL' }
  vulnVisible.value = true
}
async function saveVuln() {
  if (!vulnForm.value.cve) {
    ElMessage.warning('CVE 必填')
    return
  }
  saving.value = true
  try {
    await securityApi.createVuln(vulnForm.value)
    ElMessage.success('漏洞已登记')
    vulnVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['security'] })
    await loadVulns()
    await loadGate()
  } finally {
    saving.value = false
  }
}

async function changeStatus(row: VulnEntity, status: string) {
  await securityApi.updateVulnStatus(row.id, status)
  ElMessage.success(`状态已更新为 ${status}`)
  await loadGate()
}
async function acceptVuln(row: VulnEntity) {
  await ElMessageBox.prompt('填写豁免说明（风险接受理由）', '风险豁免', {
    confirmButtonText: '确认豁免',
    cancelButtonText: '取消',
    inputValue: row.mitigation || '',
  }).then(async ({ value }: any) => {
    await securityApi.acceptVuln(row.id, value)
    ElMessage.success('已豁免')
    await loadVulns()
    await loadGate()
  }).catch(() => {/* cancelled */})
}

function openQa() {
  qaForm.value = { version: '3.0.0', module: '', kind: 'UNIT', summary: '', coverage: 0, gate: 'PASS' }
  qaVisible.value = true
}
async function saveQa() {
  if (!qaForm.value.version) {
    ElMessage.warning('版本必填')
    return
  }
  saving.value = true
  try {
    await securityApi.addQa(qaForm.value)
    ElMessage.success('报告已保存')
    qaVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['security'] })
    await loadQa()
    await loadGate()
  } finally {
    saving.value = false
  }
}

function sevTag(s?: string) {
  if (s === 'CRITICAL') return 'danger'
  if (s === 'HIGH') return 'warning'
  if (s === 'MEDIUM') return 'info'
  return 'success'
}
function cvssColor(c?: number) {
  if (c == null) return
  if (c >= 9) return 'var(--td-danger)'
  if (c >= 7) return 'var(--td-warning)'
  if (c >= 4) return 'var(--td-text-2)'
  return 'var(--td-success)'
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
.gate-banner {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border-radius: 12px;
  margin-bottom: 16px;
  border: 1px solid;
}
.gate-banner.pass {
  background: #f0fdf4;
  border-color: #bbf7d0;
  color: #065f46;
}
.gate-banner.block,
.gate-banner.fail {
  background: #fef2f2;
  border-color: #fecaca;
  color: #991b1b;
}
.gate-title {
  font-size: 15px;
  font-weight: 600;
}
.gate-sub {
  font-size: 12px;
  margin-top: 2px;
  opacity: 0.85;
}
.sbom-box {
  background: #0f172a;
  border-radius: 12px;
  padding: 16px;
  max-height: 520px;
  overflow: auto;
}
.sbom-json {
  margin: 0;
  font-family: var(--td-font-mono);
  font-size: 11px;
  color: #e2e8f0;
  white-space: pre-wrap;
  word-break: break-all;
}
.mono {
  font-family: var(--td-font-mono);
}
.muted {
  color: var(--td-text-4);
}
</style>