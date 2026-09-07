<template>
  <div class="sec-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">安全测试与漏洞治理</h2>
        <p class="page-desc">供应链漏洞闭环（SCA/SAST）+ 质量门禁 + SBOM 制品</p>
      </div>
      <el-button :icon="Plus" type="primary" @click="openAddVuln">录入漏洞</el-button>
    </div>

    <el-card shadow="never" class="gate-card">
      <div class="gate-metrics">
        <div class="gate-item"><span class="gate-label">CRITICAL 未修复</span><span class="gate-val danger">{{ gate.criticalOpen }}</span></div>
        <div class="gate-item"><span class="gate-label">HIGH 未修复</span><span class="gate-val danger">{{ gate.highOpen }}</span></div>
        <div class="gate-item"><span class="gate-label">质量门禁</span>
          <el-tag :type="gate.gate === 'PASS' ? 'success' : 'danger'">{{ gate.gate }}</el-tag>
        </div>
      </div>
    </el-card>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <el-tab-pane label="漏洞追踪" name="vulns" />
      <el-tab-pane label="QA 报告" name="qa" />
      <el-tab-pane label="SBOM" name="sbom" />
    </el-tabs>

    <el-card v-if="tab === 'vulns'" shadow="never">
      <el-table :data="vulns" v-loading="loading" stripe>
        <el-table-column prop="packageName" label="依赖包" min-width="150" />
        <el-table-column prop="cve" label="CVE" min-width="120" />
        <el-table-column prop="cvss" label="CVSS" width="90" />
        <el-table-column prop="severity" label="严重度" width="100">
          <template #default="{ row }"><el-tag :type="sevTag(row.severity)" size="small">{{ row.severity }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="affectedVer" label="受影响版本" min-width="110" />
        <el-table-column prop="fixedVer" label="修复版本" min-width="110" />
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }"><el-tag size="small">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button v-if="['OPEN', 'FIXING'].includes(row.status)" size="small" link type="primary" @click="mark(row, 'FIXED')">标记修复</el-button>
            <el-button v-if="row.status !== 'RISK_ACCEPTED'" size="small" link type="warning" @click="acceptVuln(row)">风险接受</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="tab === 'qa'" shadow="never">
      <el-table :data="qaReports" v-loading="loading" stripe>
        <el-table-column prop="version" label="版本" width="110" />
        <el-table-column prop="module" label="模块" min-width="120" />
        <el-table-column prop="kind" label="类型" width="120" />
        <el-table-column prop="coverage" label="覆盖率" width="100">
          <template #default="{ row }">{{ row.coverage != null ? row.coverage + '%' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="gate" label="门禁" width="90">
          <template #default="{ row }"><el-tag :type="row.gate === 'PASS' ? 'success' : 'danger'" size="small">{{ row.gate }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="runTime" label="时间" min-width="150" />
        <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-card v-if="tab === 'sbom'" shadow="never">
      <div class="sbom-toolbar">
        <el-input v-model="sbomVersion" placeholder="版本号" style="width: 180px" />
        <el-button :icon="Search" @click="loadSbom">生成 SBOM</el-button>
      </div>
      <pre v-if="sbom" class="sbom-pre">{{ JSON.stringify(sbom, null, 2) }}</pre>
      <p v-else class="sbom-empty">输入版本号生成 CycloneDX 物料清单摘要</p>
    </el-card>

    <!-- 录入漏洞 -->
    <el-dialog v-model="addVulnVisible" title="录入漏洞" width="520px" :close-on-click-modal="false">
      <el-form label-width="110px">
        <el-form-item label="依赖包" required><el-input v-model="vuln.packageName" /></el-form-item>
        <el-form-item label="CVE"><el-input v-model="vuln.cve" /></el-form-item>
        <el-form-item label="CVSS"><el-input-number v-model="vuln.cvss" :min="0" :max="10" :precision="1" /></el-form-item>
        <el-form-item label="严重度">
          <el-select v-model="vuln.severity" style="width: 100%">
            <el-option v-for="s in ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="受影响版本"><el-input v-model="vuln.affectedVer" /></el-form-item>
        <el-form-item label="修复版本"><el-input v-model="vuln.fixedVer" /></el-form-item>
        <el-form-item label="来源">
          <el-select v-model="vuln.source" style="width: 100%">
            <el-option v-for="s in ['SCA', 'SAST', 'MANUAL']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVulnVisible = false">取消</el-button>
        <el-button type="primary" @click="doAddVuln">录入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { securityApi } from '@/api/modules/security'

const tab = ref('vulns')
const loading = ref(false)
const vulns = ref([])
const qaReports = ref([])
const sbom = ref(null)
const sbomVersion = ref('1.0.0')
const gate = ref({ criticalOpen: 0, highOpen: 0, gate: 'PASS' })

const addVulnVisible = ref(false)
const vuln = ref({ packageName: '', cve: '', cvss: 5, severity: 'MEDIUM', affectedVer: '', fixedVer: '', source: 'SCA' })

function sevTag(s) {
  const map = { CRITICAL: 'danger', HIGH: 'warning', MEDIUM: '', LOW: 'info' }
  return map[s] || 'info'
}

async function loadAll() {
  loading.value = true
  try {
    const [v, g, q] = await Promise.all([securityApi.vulns(), securityApi.gate(), securityApi.qa()])
    vulns.value = v || []
    gate.value = g || gate.value
    qaReports.value = q || []
  } catch (e) { ElMessage.error('加载失败') } finally { loading.value = false }
}

function onTabChange(name) { if (name === 'sbom') loadSbom() }

async function loadSbom() {
  try {
    sbom.value = await securityApi.sbom(sbomVersion.value)
  } catch (e) { ElMessage.error('SBOM 生成失败') }
}

function openAddVuln() {
  vuln.value = { packageName: '', cve: '', cvss: 5, severity: 'MEDIUM', affectedVer: '', fixedVer: '', source: 'SCA' }
  addVulnVisible.value = true
}

async function doAddVuln() {
  if (!vuln.value.packageName) { ElMessage.warning('请填写依赖包'); return }
  try {
    await securityApi.createVuln(vuln.value)
    ElMessage.success('已录入')
    addVulnVisible.value = false
    await loadAll()
  } catch (e) { ElMessage.error(e.message || '录入失败') }
}

async function mark(row, status) {
  try {
    await securityApi.updateStatus(row.id, status)
    ElMessage.success('已更新')
    await loadAll()
  } catch (e) { ElMessage.error('更新失败') }
}

async function acceptVuln(row) {
  try {
    const { value } = await ElMessageBox.prompt('填写缓解措施', '风险接受', { inputPlaceholder: '缓解措施' })
    await securityApi.accept(row.id, value)
    ElMessage.success('已风险接受')
    await loadAll()
  } catch (e) { /* cancelled */ }
}

onMounted(loadAll)
</script>

<style scoped>
.sec-page { max-width: 1280px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.page-desc { font-size: 13px; color: #9ca3af; margin: 4px 0 0; }
.gate-card { margin-bottom: 16px; }
.gate-metrics { display: flex; gap: 40px; }
.gate-item { display: flex; flex-direction: column; gap: 4px; }
.gate-label { font-size: 12px; color: #9ca3af; }
.gate-val { font-size: 22px; font-weight: 700; color: #1f2937; }
.gate-val.danger { color: #ef4444; }
.sbom-toolbar { display: flex; gap: 8px; margin-bottom: 12px; }
.sbom-pre { background: #0f172a; color: #e5e7eb; padding: 16px; border-radius: 8px; font-size: 12px; overflow: auto; }
.sbom-empty { color: #9ca3af; }
</style>