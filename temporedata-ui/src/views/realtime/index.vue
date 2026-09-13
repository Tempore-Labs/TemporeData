<template>
  <div class="page">
    <PageHeader title="实时任务" subtitle="流式计算 / 实时任务管理（Flink 等）">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建实时任务</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="name" label="任务名称" min-width="180">
        <template #default="{ row }"><span class="mono">{{ row.name || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="120">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.type || '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="clusterId" label="集群" width="120">
        <template #default="{ row }"><span class="mono">{{ row.clusterId || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="150">
        <template #default="{ row }"><span class="mono">{{ row.createTime || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="330" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text :disabled="row.status === 'RUNNING'" @click="start(row)">启动</el-button>
          <el-button size="small" text :disabled="row.status !== 'RUNNING'" @click="stop(row)">停止</el-button>
          <el-button size="small" text type="primary" @click="savepoint(row)">Savepoint</el-button>
          <el-button size="small" text type="info" @click="viewLogs(row)">日志</el-button>
          <el-button size="small" text type="info" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <!-- Runtime logs drawer -->
    <el-drawer v-model="logVisible" :title="`运行日志 · ${logTargetName}`" size="50%">
      <el-skeleton v-if="logLoading" :rows="8" animated />
      <el-empty v-else-if="!logs.length" description="暂无日志（启动/停止/生成 Savepoint 后产生）" />
      <div v-else class="log-box">
        <div v-for="(l, i) in logs" :key="i" class="log-line">
          <span class="mono">{{ l }}</span>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑实时任务' : '新建实时任务'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="s in ['FLINK', 'KAFKA', 'SPARK_STREAMING', 'OTHER']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="集群ID">
          <el-input v-model="form.clusterId" />
        </el-form-item>
        <el-form-item label="脚本">
          <el-input v-model="form.script" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ['STOPPED', 'RUNNING', 'FAILED', 'DEGRADED']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
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
import { realtimeApi, type RealEntity } from '@/api/ops2.module'

const queryClient = useQueryClient()
const rows = ref<RealEntity[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<RealEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<RealEntity>>({})

const logVisible = ref(false)
const logLoading = ref(false)
const logTargetName = ref('')
const logs = ref<string[]>([])

async function load() {
  loading.value = true
  try {
    rows.value = (await realtimeApi.list()) || []
  } finally {
    loading.value = false
  }
}
load()

function openCreate() {
  editing.value = null
  form.value = { name: '', type: 'FLINK', status: 'STOPPED' }
  dialogVisible.value = true
}
function openEdit(row: RealEntity) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.name) {
    ElMessage.warning('名称必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) await realtimeApi.update(editing.value.id, form.value)
    else await realtimeApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['realtime'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function start(row: RealEntity) {
  try {
    await realtimeApi.start(row.id)
    ElMessage.success('已启动')
  } catch {
    // Start can now honestly fail (e.g. compute engine not installed); the HTTP
    // interceptor already surfaced the error - do not claim success here.
  } finally {
    await load()
  }
}
async function stop(row: RealEntity) {
  try {
    await realtimeApi.stop(row.id)
    ElMessage.success('已停止')
  } catch {
    // engine stop may fail (no running job id recorded); interceptor shows the reason.
  } finally {
    await load()
  }
}
async function savepoint(row: RealEntity) {
  const res = await realtimeApi.savepoint(row.id)
  const path = (res as any)?.savepointPath
  ElMessage.success(path ? `Savepoint 已生成：${path}` : '已生成 Savepoint')
  await load()
}

async function viewLogs(row: RealEntity) {
  logVisible.value = true
  logLoading.value = true
  logTargetName.value = row.name || row.id
  logs.value = []
  try {
    logs.value = (await realtimeApi.logs(row.id)) || []
  } finally {
    logLoading.value = false
  }
}
async function remove(row: RealEntity) {
  await ElMessageBox.confirm(`确认删除实时任务 ${row.name} ？`, '提示', { type: 'warning' })
  await realtimeApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page { padding: 24px; }
.mono { font-family: var(--td-font-mono); }
.log-box {
  font-size: 12px;
  background: var(--td-bg-2);
  border-radius: 8px;
  padding: 12px;
}
.log-line {
  padding: 3px 0;
  border-bottom: 1px solid var(--td-border);
  white-space: pre-wrap;
}
.log-box .log-line:last-child { border-bottom: none; }
</style>