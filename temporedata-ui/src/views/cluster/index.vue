<template>
  <div class="page">
    <PageHeader title="计算集群" subtitle="计算引擎集群管理与 Agent 状态监控">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 添加集群</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else class="cluster-grid">
      <Card v-for="c in clusters" :key="c.id" class="cluster-card" hoverable>
        <div class="c-head">
          <div class="c-icon"><el-icon :size="18"><Monitor /></el-icon></div>
          <div class="c-meta">
            <div class="c-name mono">{{ c.name }}</div>
            <div class="c-type">{{ c.type || '—' }} · {{ c.masterUrl || (c.host || '—') }}</div>
          </div>
          <span class="c-status"><StatusBadge :status="c.status || 'unknown'" :text="c.status || '—'" /></span>
        </div>

        <!-- resources -->
        <div class="c-resources">
          <div class="res-item">
            <span class="res-label">CPU</span>
            <el-progress :percentage="Math.round(c.cpuUsage ?? 0)" :stroke-width="6" :color="usageColor(c.cpuUsage)" />
            <span class="res-val mono">{{ c.cpuUsage?.toFixed(0) ?? '—' }}%</span>
          </div>
          <div class="res-item">
            <span class="res-label">内存</span>
            <el-progress :percentage="Math.round(c.memoryUsage ?? 0)" :stroke-width="6" :color="usageColor(c.memoryUsage)" />
            <span class="res-val mono">{{ c.memoryUsage?.toFixed(0) ?? '—' }}%</span>
          </div>
          <div class="res-item">
            <span class="res-label">磁盘</span>
            <el-progress :percentage="Math.round(c.diskUsage ?? 0)" :stroke-width="6" :color="usageColor(c.diskUsage)" />
            <span class="res-val mono">{{ c.diskUsage?.toFixed(0) ?? '—' }}%</span>
          </div>
        </div>

        <div class="c-foot">
          <span class="c-agent">
            Agent:
            <StatusBadge :status="c.agentStatus || 'offline'" :text="c.agentStatus || 'offline'" />
          </span>
          <span class="c-nodes">节点 {{ c.nodeCount ?? 0 }}</span>
          <div class="c-actions">
            <el-button size="small" text type="primary" @click="viewNodes(c)">节点</el-button>
            <el-button size="small" text type="primary" @click="openEdit(c)">编辑</el-button>
            <el-button size="small" text type="danger" @click="delCluster(c)">删除</el-button>
          </div>
        </div>
      </Card>
      <EmptyState v-if="clusters.length === 0" title="暂无集群" desc="点击右上角添加计算集群" icon="Monitor" />
    </div>

    <!-- create/edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑集群' : '添加集群'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="t in ['YARN', 'K8S', 'SPARK', 'FLINK', 'HIVE', 'STANDALONE']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="Master URL">
          <el-input v-model="form.masterUrl" placeholder="如 yarn://master:8032" />
        </el-form-item>
        <el-form-item label="主机">
          <el-input v-model="form.host" />
        </el-form-item>
        <el-form-item label="SSH 端口">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="SSH 用户">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="SSH 密码">
          <el-input v-model="form.password" type="password" show-password placeholder="留空则不修改" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ['RUNNING', 'PAUSED', 'STOPPED', 'ERROR']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- nodes dialog -->
    <el-dialog v-model="nodeVisible" :title="`节点列表 · ${nodeTargetName}`" width="680px">
      <div class="node-bar">
        <el-button size="small" type="primary" @click="addNodeVisible = true">+ 添加节点</el-button>
      </div>
      <el-skeleton v-if="nodeLoading" :rows="5" animated />
      <DataTable v-else :data="nodes">
        <el-table-column prop="nodeId" label="节点 ID" min-width="180">
          <template #default="{ row }"><span class="mono">{{ row.nodeId || row.id || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="host" label="主机" min-width="140">
          <template #default="{ row }"><span class="mono">{{ row.host || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="agentStatus" label="Agent" width="120">
          <template #default="{ row }"><StatusBadge :status="row.agentStatus || 'offline'" :text="row.agentStatus || 'offline'" /></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="install(row)">安装 Agent</el-button>
            <el-button size="small" text type="primary" @click="checkStatus(row)">状态</el-button>
          </template>
        </el-table-column>
      </DataTable>
    </el-dialog>

    <!-- add node dialog -->
    <el-dialog v-model="addNodeVisible" title="添加节点" width="460px">
      <el-form :model="nodeForm" label-width="100px">
        <el-form-item label="节点 ID" required>
          <el-input v-model="nodeForm.nodeId" />
        </el-form-item>
        <el-form-item label="主机">
          <el-input v-model="nodeForm.host" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addNodeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doAddNode">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import Card from '@/components/base/Card.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { clusterApi, type ClusterEntity } from '@/api/cluster'

const queryClient = useQueryClient()
const clusters = ref<ClusterEntity[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const editing = ref<ClusterEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<ClusterEntity>>({})

const nodeVisible = ref(false)
const nodeLoading = ref(false)
const nodeTargetId = ref('')
const nodeTargetName = ref('')
const nodes = ref<Record<string, unknown>[]>([])
const addNodeVisible = ref(false)
const nodeForm = ref<Record<string, unknown>>({})

async function load() {
  loading.value = true
  try {
    clusters.value = await clusterApi.list()
  } finally {
    loading.value = false
  }
}
load()

function openCreate() {
  editing.value = null
  form.value = { name: '', type: 'YARN', masterUrl: '', host: '', port: 22, username: '', password: '', status: 'RUNNING' }
  dialogVisible.value = true
}
function openEdit(c: ClusterEntity) {
  editing.value = c
  form.value = { ...c, password: '' }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.name) {
    ElMessage.warning('名称必填')
    return
  }
  saving.value = true
  try {
    const payload = { ...form.value } as Partial<ClusterEntity>
    if (!payload.password) delete payload.password
    if (editing.value) await clusterApi.update(editing.value.id, payload)
    else await clusterApi.create(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['cluster'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function delCluster(c: ClusterEntity) {
  await ElMessageBox.confirm(`确认删除集群 ${c.name} ？`, '提示', { type: 'warning' })
  await clusterApi.remove(c.id)
  ElMessage.success('已删除')
  await load()
}

async function viewNodes(c: ClusterEntity) {
  nodeTargetId.value = c.id
  nodeTargetName.value = c.name || c.id
  nodeVisible.value = true
  nodeLoading.value = true
  try {
    nodes.value = await clusterApi.nodes(c.id)
  } finally {
    nodeLoading.value = false
  }
}
async function doAddNode() {
  if (!nodeForm.value.nodeId) {
    ElMessage.warning('节点 ID 必填')
    return
  }
  saving.value = true
  try {
    await clusterApi.addNode(nodeTargetId.value, nodeForm.value)
    ElMessage.success('节点已添加')
    addNodeVisible.value = false
    nodeForm.value = {}
    nodes.value = await clusterApi.nodes(nodeTargetId.value)
  } finally {
    saving.value = false
  }
}
async function install(row: Record<string, unknown>) {
  const id = row.nodeId || row.id
  if (!id) {
    ElMessage.warning('无效节点 ID')
    return
  }
  const res = await clusterApi.installAgent(String(id))
  ElMessage.success(`Agent 安装: ${JSON.stringify(res)?.slice(0, 80) || 'ok'}`)
}
async function checkStatus(row: Record<string, unknown>) {
  const id = row.nodeId || row.id
  if (!id) return
  const res = await clusterApi.nodeStatus(String(id))
  ElMessage.info(`节点状态: ${JSON.stringify(res)?.slice(0, 120) || '—'}`)
}

function usageColor(v?: number) {
  if (v == null) return '#cbd5e1'
  if (v >= 85) return 'var(--td-danger)'
  if (v >= 60) return 'var(--td-warning)'
  return 'var(--td-success)'
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.cluster-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}
.cluster-card {
  padding: 18px;
}
.c-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.c-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: var(--td-primary-soft);
  color: var(--td-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.c-meta {
  flex: 1;
  min-width: 0;
}
.c-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--td-text-1);
}
.c-type {
  font-size: 11px;
  color: var(--td-text-4);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.c-resources {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}
.res-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.res-label {
  width: 32px;
  font-size: 11px;
  color: var(--td-text-4);
  flex-shrink: 0;
}
.res-item .el-progress {
  flex: 1;
}
.res-val {
  width: 40px;
  text-align: right;
  font-size: 11px;
  color: var(--td-text-2);
}
.c-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  border-top: 1px solid var(--td-border-light);
  padding-top: 12px;
}
.c-agent,
.c-nodes {
  font-size: 11px;
  color: var(--td-text-3);
}
.c-actions {
  margin-left: auto;
}
.node-bar {
  margin-bottom: 12px;
}
.mono {
  font-family: var(--td-font-mono);
}
</style>