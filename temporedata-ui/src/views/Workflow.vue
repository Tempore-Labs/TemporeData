<template>
  <div class="workflow-page">
    <!-- Left: Workflow list -->
    <div class="workflow-sidebar">
      <div class="sidebar-header">
        <h3>作业流列表</h3>
        <el-button type="primary" size="small" :icon="Plus" @click="openCreateDialog">新建</el-button>
      </div>
      <el-input v-model="searchKey" placeholder="搜索作业流..." size="small" clearable class="sidebar-search" />
      <div class="workflow-list" v-loading="loading">
        <div
          v-for="wf in filteredWorkflows"
          :key="wf.id"
          class="workflow-item"
          :class="{ active: currentWorkflow?.id === wf.id }"
          @click="selectWorkflow(wf)"
        >
          <div class="wf-name">{{ wf.name }}</div>
          <div class="wf-meta">
            <el-tag :type="statusTag(wf.status)" size="small">{{ wf.status === 'ONLINE' ? '在线' : '离线' }}</el-tag>
          </div>
        </div>
        <el-empty v-if="!filteredWorkflows.length" description="暂无作业流" :image-size="60" />
      </div>
    </div>

    <!-- Right: DAG Editor -->
    <div class="workflow-main">
      <template v-if="currentWorkflow">
        <!-- Header -->
        <div class="main-header">
          <div class="header-info">
            <h2 class="wf-title">{{ currentWorkflow.name }}</h2>
            <span class="wf-desc" v-if="currentWorkflow.description">{{ currentWorkflow.description }}</span>
          </div>
          <div class="header-actions">
            <el-button type="primary" :icon="VideoPlay" @click="handleExecute" :loading="executing">执行</el-button>
            <el-button :icon="Tickets" @click="openInstances">运行实例</el-button>
            <el-button :icon="Share" @click="openLineage">血缘</el-button>
            <el-button :icon="Upload" @click="openImport">导入</el-button>
            <el-dropdown trigger="click" @command="onIoCommand">
              <el-button :icon="Download">导出</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="export-json" :disabled="!currentWorkflow">导出 JSON</el-dropdown-item>
                  <el-dropdown-item command="export-excel" :disabled="!currentWorkflow">导出 Excel</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button :icon="Edit" @click="openEditDialog">编辑信息</el-button>
            <el-button :icon="Clock" @click="scheduleVisible = true">调度</el-button>
            <el-button type="danger" :icon="Delete" @click="handleDeleteWorkflow">删除</el-button>
          </div>
        </div>

        <!-- DAG Editor -->
        <div class="dag-wrapper">
          <WorkflowDagEditor
            ref="dagEditorRef"
            :workflowNodes="dagNodes"
            :workflowEdges="dagEdges"
            :datasources="datasources"
            @change="onDagChange"
            @reload="reloadWorkflow"
          />
        </div>

        <!-- Save bar -->
        <div class="save-bar" v-if="dagDirty">
          <el-alert title="DAG 已修改，请保存" type="warning" :closable="false" show-icon />
          <el-button type="primary" :loading="saving" @click="saveDag">保存 DAG</el-button>
        </div>
      </template>

      <div v-else class="no-workflow">
        <el-empty description="请选择或创建一个作业流" :image-size="80" />
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑作业流' : '新建作业流'" width="480px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入作业流名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" maxlength="256" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="handleDialogSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Schedule Dialog -->
    <el-dialog v-model="scheduleVisible" title="调度配置" width="420px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="Cron 表达式">
          <el-input v-model="cronExpression" placeholder="0 0 2 * * ?" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="scheduleEnabled"
            active-text="启用"
            inactive-text="停用"
            @change="handleSchedule"
            :loading="scheduleLoading"
          />
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- Run Instances Dialog -->
    <el-dialog v-model="instVisible" title="运行实例" width="860px" :close-on-click-modal="false">
      <el-table :data="instances" v-loading="instLoading" stripe max-height="360">
        <el-table-column prop="id" label="实例 ID" min-width="200" show-overflow-tooltip />
        <el-table-column prop="triggerType" label="触发" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="execStatusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="160" />
        <el-table-column prop="finishTime" label="结束时间" min-width="160">
          <template #default="{ row }">{{ row.finishTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="resultMsg" label="结果" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.resultMsg || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showInstanceDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!instances.length && !instLoading" description="暂无运行实例" :image-size="60" />
    </el-dialog>

    <!-- Instance Detail Dialog (node statuses) -->
    <el-dialog v-model="instDetailVisible" :title="`实例详情 - ${instDetailId}`" width="780px" :close-on-click-modal="false">
      <el-table :data="detailNodes" v-loading="instDetailLoading" stripe max-height="320">
        <el-table-column prop="nodeName" label="节点" min-width="140" show-overflow-tooltip />
        <el-table-column prop="nodeType" label="类型" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="execStatusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryTimes" label="重试" width="70" align="center" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" align="center" />
        <el-table-column prop="errorMsg" label="信息" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.result || row.errorMsg || '-' }}</template>
        </el-table-column>
        <el-table-column label="日志" width="70" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showNodeLogs(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="runtime-actions">
          <el-tag :type="execStatusTag(instDetailStatus)" effect="dark" size="small">{{ instDetailStatus }}</el-tag>
          <span class="spacer"></span>
          <el-button v-if="instDetailStatus === 'RUNNING'" size="small" @click="doPause">暂停</el-button>
          <el-button v-if="instDetailStatus === 'PAUSED'" size="small" type="success" @click="doResume">继续</el-button>
          <el-button v-if="instDetailStatus === 'RUNNING' || instDetailStatus === 'PAUSED'" size="small" type="danger" @click="doStop">停止</el-button>
          <el-button v-if="['SUCCESS', 'FAILED', 'STOPPED'].includes(instDetailStatus)" size="small" type="primary" @click="doRerun">重跑</el-button>
          <el-button size="small" text @click="refreshInstanceDetail">刷新</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Node Logs Dialog -->
    <el-dialog v-model="nodeLogVisible" :title="nodeLogTitle" width="640px" :close-on-click-modal="false">
      <pre v-loading="nodeLogLoading" class="log-pre">{{ nodeLogText || '暂无日志' }}</pre>
    </el-dialog>

    <!-- Task Lineage Dialog -->
    <el-dialog v-model="lineageVisible" :title="`任务级血缘 - ${currentWorkflow?.name || ''}`" width="760px" :close-on-click-modal="false">
      <div class="lineage-import" v-loading="lineageLoading">
        <el-alert v-if="lineageSynced" type="success" :closable="false" show-icon
                  :title="`已解析 ${lineageRows} 条表依赖`" style="margin-bottom: 12px" />
        <el-table v-if="lineageTasks.length" :data="lineageTasks" stripe max-height="400">
          <el-table-column prop="name" label="节点" min-width="140" show-overflow-tooltip />
          <el-table-column label="读取表(输入)" min-width="200">
            <template #default="{ row }">
              <el-tag v-for="t in row.reads" :key="t" size="small" type="info" style="margin: 2px 4px 2px 0">{{ t }}</el-tag>
              <span v-if="!row.reads.length">-</span>
            </template>
          </el-table-column>
          <el-table-column label="写入表(输出)" min-width="200">
            <template #default="{ row }">
              <el-tag v-for="t in row.writes" :key="t" size="small" type="success" style="margin: 2px 4px 2px 0">{{ t }}</el-tag>
              <span v-if="!row.writes.length">-</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!lineageLoading && !lineageTasks.length" description="暂无任务级血缘" :image-size="60" />
      </div>
      <template #footer>
        <el-button type="primary" :loading="lineageLoading" @click="openLineage">重新解析</el-button>
        <el-button @click="lineageVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- Import Dialog (Excel / JSON) -->
    <el-dialog v-model="importVisible" title="导入工作流" width="620px" :close-on-click-modal="false">
      <el-tabs v-model="importTab" type="card">
        <el-tab-pane label="Excel 导入" name="excel">
          <p class="import-hint">上传符合模板的 .xlsx（Sheet：节点 / 边 / 调度）。可先导出已有工作流的 Excel 作模板。</p>
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleExcelPicked"
            :on-remove="() => (excelFile = null)"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
          </el-upload>
          <div style="margin-top: 12px; text-align: right">
            <el-button :loading="importing" :disabled="!excelFile" @click="doExcelImport">开始导入</el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="JSON / Python" name="json">
          <p class="import-hint">粘贴 Python SDK / JSON 生成的工作流定义（含 name、nodes、edges）。</p>
          <el-input v-model="jsonText" type="textarea" :rows="8" placeholder='{"name":"my_flow","description":"","nodes":[...],"edges":[...]}' />
          <div style="margin-top: 12px; text-align: right">
            <el-button :loading="importing" :disabled="!jsonText.trim()" @click="doJsonImport">导入</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, VideoPlay, Clock, Tickets, Share, Upload, Download, UploadFilled } from '@element-plus/icons-vue'
import { workflowApi } from '@/api/modules/workflow'
import { workflowLineageApi } from '@/api/modules/workflowLineage'
import { workflowImportApi } from '@/api/modules/workflowImport'
import { datasourceApi } from '@/api/modules/datasource'
import { ElMessage, ElMessageBox } from 'element-plus'
import WorkflowDagEditor from '@/components/WorkflowDagEditor.vue'

// ---- Workflow list ----
const workflows = ref([])
const loading = ref(false)
const currentWorkflow = ref(null)
const searchKey = ref('')
const datasources = ref([])

const filteredWorkflows = computed(() => {
  if (!searchKey.value) return workflows.value
  const kw = searchKey.value.toLowerCase()
  return workflows.value.filter(w => w.name?.toLowerCase().includes(kw))
})

// ---- DAG state ----
const dagNodes = ref([])
const dagEdges = ref([])
const dagDirty = ref(false)
const dagChangeData = ref(null)
const saving = ref(false)
const dagEditorRef = ref(null)

// ---- Dialog ----
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogSubmitting = ref(false)
const formRef = ref(null)
const form = reactive({ name: '', description: '' })
const rules = {
  name: [{ required: true, message: '请输入作业流名称', trigger: 'blur' }]
}

// ---- Schedule ----
const scheduleVisible = ref(false)
const scheduleEnabled = ref(false)
const scheduleLoading = ref(false)
const cronExpression = ref('')

// ---- Execution ----
const executing = ref(false)

// ---- Run instances ----
const instVisible = ref(false)
const instLoading = ref(false)
const instances = ref([])
const instDetailVisible = ref(false)
const instDetailLoading = ref(false)
const instDetailId = ref('')
const instDetailStatus = ref('')
const detailNodes = ref([])
const nodeLogVisible = ref(false)
const nodeLogLoading = ref(false)
const nodeLogText = ref('')
const nodeLogTitle = ref('')

// ---- Status helpers ----
function statusTag(status) {
  return status === 'ONLINE' ? 'success' : 'info'
}
function execStatusTag(status) {
  const map = {
    RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger',
    ABORTED: 'info', SKIPPED: 'info', PAUSED: 'warning',
    STOPPED: 'danger', PENDING: 'info'
  }
  return map[status] || 'info'
}

// ---- Fetch ----
onMounted(async () => {
  await fetchWorkflows()
  await fetchDatasources()
})

async function fetchWorkflows() {
  loading.value = true
  try {
    workflows.value = await workflowApi.list() || []
  } catch (e) {
    ElMessage.error('获取作业流列表失败')
  } finally { loading.value = false }
}

async function fetchDatasources() {
  try {
    datasources.value = await datasourceApi.list() || []
  } catch (e) { /* ignore */ }
}

// ---- Select workflow ----
async function selectWorkflow(wf) {
  currentWorkflow.value = wf
  try {
    const detail = await workflowApi.detail(wf.id)
    currentWorkflow.value = detail
    dagNodes.value = detail.nodes || []
    dagEdges.value = detail.edges || []
    dagDirty.value = false
    scheduleEnabled.value = detail.status === 'ONLINE'
    cronExpression.value = detail.cronExpression || ''
  } catch (e) {
    ElMessage.error('加载作业流详情失败')
  }
}

async function reloadWorkflow() {
  if (currentWorkflow.value) {
    await selectWorkflow(currentWorkflow.value)
  }
}

// ---- DAG changes ----
function onDagChange({ nodes, edges }) {
  dagChangeData.value = { nodes, edges }
  dagDirty.value = true
}

async function saveDag() {
  if (!currentWorkflow.value || !dagChangeData.value) return
  saving.value = true
  try {
    const payload = {
      name: currentWorkflow.value.name,
      description: currentWorkflow.value.description,
      nodes: dagChangeData.value.nodes,
      edges: dagChangeData.value.edges
    }
    await workflowApi.update(currentWorkflow.value.id, payload)
    dagDirty.value = false
    dagChangeData.value = null
    ElMessage.success('DAG 保存成功')
    await reloadWorkflow()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  } finally { saving.value = false }
}

// ---- CRUD ----
function openCreateDialog() {
  isEdit.value = false
  form.name = ''
  form.description = ''
  formRef.value?.resetFields()
  dialogVisible.value = true
}

function openEditDialog() {
  if (!currentWorkflow.value) return
  isEdit.value = true
  form.name = currentWorkflow.value.name
  form.description = currentWorkflow.value.description || ''
  dialogVisible.value = true
}

async function handleDialogSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  dialogSubmitting.value = true
  try {
    if (isEdit.value) {
      await workflowApi.update(currentWorkflow.value.id, { name: form.name, description: form.description, nodes: dagNodes.value, edges: dagEdges.value })
      ElMessage.success('作业流已更新')
    } else {
      await workflowApi.create({ name: form.name, description: form.description, nodes: [], edges: [] })
      ElMessage.success('作业流已创建')
    }
    dialogVisible.value = false
    await fetchWorkflows()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally { dialogSubmitting.value = false }
}

async function handleDeleteWorkflow() {
  if (!currentWorkflow.value) return
  try {
    await ElMessageBox.confirm('确定删除该作业流？', '确认', { type: 'warning' })
    await workflowApi.delete(currentWorkflow.value.id)
    ElMessage.success('已删除')
    currentWorkflow.value = null
    dagNodes.value = []
    dagEdges.value = []
    dagDirty.value = false
    await fetchWorkflows()
  } catch (e) { /* cancelled */ }
}

// ---- Execute (real DAG runtime) ----
async function handleExecute() {
  if (!currentWorkflow.value) return
  executing.value = true
  try {
    const res = await workflowApi.run(currentWorkflow.value.id)
    if (res.status === 'SUCCESS') {
      ElMessage.success('执行成功')
    } else {
      ElMessage.error(`执行失败：${res.status}`)
    }
    if (res.instanceId) {
      instDetailId.value = res.instanceId
      instDetailStatus.value = res.status || ''
      detailNodes.value = (res.nodeResults || []).map(n => ({
        nodeName: n.nodeName,
        nodeType: n.nodeName,
        status: n.status,
        retryTimes: 0,
        durationMs: n.durationMs,
        result: n.errorMsg || n.status,
        errorMsg: n.errorMsg,
        nodeInstanceId: n.nodeId
      }))
      instDetailVisible.value = true
    }
  } catch (e) {
    ElMessage.error('执行失败：' + (e.message || ''))
  } finally { executing.value = false }
}

// ---- Run instances panel ----
async function openInstances() {
  if (!currentWorkflow.value) return
  instVisible.value = true
  instLoading.value = true
  try {
    instances.value = await workflowApi.instances(currentWorkflow.value.id) || []
  } catch (e) {
    ElMessage.error('加载实例失败')
  } finally { instLoading.value = false }
}

async function showInstanceDetail(row) {
  instDetailId.value = row.id
  instDetailStatus.value = row.status || ''
  instDetailVisible.value = true
  instDetailLoading.value = true
  detailNodes.value = []
  try {
    const detail = await workflowApi.instanceDetail(row.id)
    instDetailStatus.value = detail.status || instDetailStatus.value
    detailNodes.value = detail.nodes || []
  } catch (e) {
    ElMessage.error('加载实例详情失败')
  } finally { instDetailLoading.value = false }
}

async function refreshInstanceDetail() {
  showInstanceDetail({ id: instDetailId.value, status: instDetailStatus.value })
}

// ---- Runtime management (P0-3) ----
async function doPause() {
  try {
    await workflowApi.pauseInstance(instDetailId.value)
    instDetailStatus.value = 'PAUSED'
    ElMessage.success('已提交暂停')
  } catch (e) {
    ElMessage.error(e.message || '暂停失败')
  }
}

async function doResume() {
  try {
    await workflowApi.resumeInstance(instDetailId.value)
    instDetailStatus.value = 'RUNNING'
    ElMessage.success('已提交继续')
  } catch (e) {
    ElMessage.error(e.message || '继续失败')
  }
}

async function doStop() {
  try {
    await ElMessageBox.confirm('确定停止该实例？', '确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await workflowApi.stopInstance(instDetailId.value)
    instDetailStatus.value = 'STOPPED'
    ElMessage.success('已提交停止')
    await refreshInstances()
  } catch (e) {
    ElMessage.error(e.message || '停止失败')
  }
}

async function doRerun() {
  try {
    await ElMessageBox.confirm('确定重跑该工作流？将生成一个新实例。', '确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await workflowApi.rerunInstance(instDetailId.value, { scope: 'ALL' })
    ElMessage.success(`已重跑，新实例 ${res.instanceId}`)
    await refreshInstances()
  } catch (e) {
    ElMessage.error(e.message || '重跑失败')
  }
}

async function refreshInstances() {
  if (!currentWorkflow.value) return
  try {
    instances.value = await workflowApi.instances(currentWorkflow.value.id) || []
  } catch { /* ignore */ }
}

// ---- Task-level lineage (P1-5) ----
const lineageVisible = ref(false)
const lineageLoading = ref(false)
const lineageSynced = ref(false)
const lineageRows = ref(0)
const lineageTasks = ref([])

async function openLineage() {
  if (!currentWorkflow.value) return
  lineageVisible.value = true
  lineageLoading.value = true
  lineageTasks.value = []
  try {
    lineageRows.value = await workflowLineageApi.sync(currentWorkflow.value.id)
    lineageSynced.value = true
    const graph = await workflowLineageApi.graph(currentWorkflow.value.id)
    const nodes = graph.nodes || []
    const edges = graph.edges || []
    const taskNodes = nodes.filter(n => n.nodeType === 'TASK')
    const byId = {}
    nodes.forEach(n => { byId[n.id] = n })
    lineageTasks.value = taskNodes.map(t => ({
      name: t.name,
      reads: edges.filter(e => e.target === t.id && byId[e.source]?.nodeType === 'TABLE').map(e => (byId[e.source] || {}).name).filter(Boolean),
      writes: edges.filter(e => e.source === t.id && byId[e.target]?.nodeType === 'TABLE').map(e => (byId[e.target] || {}).name).filter(Boolean)
    }))
  } catch (e) {
    ElMessage.error('血缘解析失败：' + (e.message || ''))
  } finally { lineageLoading.value = false }
}

// ---- Import / Export (P1-7) ----
const importVisible = ref(false)
const importTab = ref('excel')
const importing = ref(false)
const excelFile = ref(null)
const jsonText = ref('')

function openImport() {
  importVisible.value = true
  importTab.value = 'excel'
  excelFile.value = null
  jsonText.value = ''
}

function handleExcelPicked(file) {
  excelFile.value = file.raw
}

async function doExcelImport() {
  if (!excelFile.value) return
  importing.value = true
  try {
    const res = await workflowImportApi.importExcel(excelFile.value)
    ElMessage.success(`导入成功：${res.workflowId}（${res.nodeCount} 节点 / ${res.edgeCount} 边）`)
    importVisible.value = false
    await fetchWorkflows()
    const created = workflows.value.find(w => w.id === res.workflowId)
    if (created) await selectWorkflow(created)
  } catch (e) {
    ElMessage.error('导入失败：' + (e.message || ''))
  } finally { importing.value = false }
}

async function doJsonImport() {
  if (!jsonText.value.trim()) return
  importing.value = true
  try {
    const parsed = JSON.parse(jsonText.value)
    const res = await workflowImportApi.importJson({
      name: parsed.name,
      description: parsed.description,
      nodes: parsed.nodes || [],
      edges: parsed.edges || []
    })
    ElMessage.success(`导入成功：${res.id}`)
    importVisible.value = false
    await fetchWorkflows()
    const created = workflows.value.find(w => w.id === res.id)
    if (created) await selectWorkflow(created)
  } catch (e) {
    ElMessage.error('导入失败：' + (e.message || 'JSON 格式错误'))
  } finally { importing.value = false }
}

async function onIoCommand(cmd) {
  if (!currentWorkflow.value) return
  if (cmd === 'export-json') {
    try {
      const data = await workflowImportApi.exportJson(currentWorkflow.value.id)
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${currentWorkflow.value.name}.workflow.json`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      ElMessage.error('导出失败：' + (e.message || ''))
    }
  } else if (cmd === 'export-excel') {
    try {
      await workflowImportApi.exportExcel(currentWorkflow.value.id)
      ElMessage.success('已导出 Excel')
    } catch (e) {
      ElMessage.error('导出失败：' + (e.message || ''))
    }
  }
}

async function showNodeLogs(node) {
  nodeLogTitle.value = node.nodeName + ' · 日志'
  nodeLogText.value = ''
  nodeLogVisible.value = true
  nodeLogLoading.value = true
  try {
    const logs = await workflowApi.instanceLogs(instDetailId.value) || []
    const filtered = node.nodeInstanceId
      ? logs.filter(l => l.nodeInstanceId === node.nodeInstanceId)
      : logs
    nodeLogText.value = filtered.map(l => `[${l.createTime}] [${l.level}] ${l.message}`).join('\n')
  } catch (e) {
    nodeLogText.value = '加载日志失败'
  } finally { nodeLogLoading.value = false }
}

async function handleSchedule() {
  if (!currentWorkflow.value) return
  scheduleLoading.value = true
  try {
    if (scheduleEnabled.value) {
      await workflowApi.schedule(currentWorkflow.value.id, { enabled: 1, cronExpression: cronExpression.value })
      ElMessage.success('调度已启用')
    } else {
      await workflowApi.disableSchedule(currentWorkflow.value.id)
      ElMessage.success('调度已停用')
    }
    await reloadWorkflow()
  } catch (e) {
    ElMessage.error('调度配置失败')
    scheduleEnabled.value = !scheduleEnabled.value
  } finally { scheduleLoading.value = false }
}
</script>

<style scoped>
.workflow-page {
  display: flex;
  height: calc(100vh - 60px);
  gap: 0;
  margin: -20px -24px;
}

/* Sidebar */
.workflow-sidebar {
  width: 240px;
  min-width: 240px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px;
  border-bottom: 1px solid #e5e7eb;
}
.sidebar-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}
.sidebar-search {
  padding: 8px 12px;
}
.workflow-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}
.workflow-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: background .15s;
}
.workflow-item:hover { background: #f3f4f6; }
.workflow-item.active {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
}
.wf-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 4px;
}
.wf-meta { display: flex; gap: 4px; }

/* Main */
.workflow-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fafbfc;
}
.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}
.header-info { display: flex; flex-direction: column; gap: 2px; }
.wf-title { margin: 0; font-size: 16px; font-weight: 600; color: #1f2937; }
.wf-desc { font-size: 12px; color: #9ca3af; }
.header-actions { display: flex; gap: 8px; }
.dag-and-monitor {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}
.dag-wrapper {
  flex: 1;
  padding: 12px;
  overflow: hidden;
  min-width: 0;
}
.save-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 8px 16px;
  background: #fffbeb;
  border-top: 1px solid #fde68a;
}
.no-workflow {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.log-pre {
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  line-height: 1.6;
  background: #0f172a;
  color: #e2e8f0;
  padding: 14px;
  border-radius: 8px;
  max-height: 420px;
  overflow-y: auto;
}

.runtime-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.runtime-actions .spacer {
  flex: 1;
}

.import-hint {
  color: #6b7280;
  font-size: 12px;
  margin: 0 0 10px;
}
</style>