<template>
  <div class="wsw">
    <!-- left: workflow list -->
    <aside class="wsw-list">
      <div class="wsw-list-head">
        <span class="wsw-list-title">工作流</span>
        <el-button size="small" type="primary" plain @click="onNew">+ 新建</el-button>
      </div>
      <div class="wsw-list-body">
        <el-skeleton v-if="loading" :rows="6" animated />
        <template v-else>
          <div
            v-for="wf in list"
            :key="wf.id"
            class="wsw-item"
            :class="{ active: wf.id === current?.id }"
            @click="onSelect(wf)"
          >
            <div class="wsw-item-main">
              <span class="wsw-item-name">{{ wf.name }}</span>
              <StatusBadge :status="wf.status || 'DRAFT'" />
            </div>
            <div class="wsw-item-sub">
              <span>{{ wf.nodes?.length ?? 0 }} 节点</span>
              <span class="mono muted">{{ wf.createTime || '—' }}</span>
              <el-button size="small" text type="danger" @click.stop="onDelete(wf)">删</el-button>
            </div>
          </div>
          <el-empty v-if="!list.length" description="还没有工作流，点击上方新建" :image-size="70" />
        </template>
      </div>
    </aside>

    <!-- right: header (name rename / new) + tabbed workspace -->
    <section class="wsw-main">
      <div class="wsw-header">
        <el-radio-group v-model="tab" size="small">
          <el-radio-button :value="'design'">设计</el-radio-button>
          <el-radio-button :value="'schedule'" :disabled="!current?.id">调度</el-radio-button>
          <el-radio-button :value="'instance'" :disabled="!current?.id">实例</el-radio-button>
          <el-radio-button :value="'monitor'" :disabled="!current?.id">监控</el-radio-button>
        </el-radio-group>
        <span class="wsw-header-tag">工作流名</span>
        <el-input
          v-model="wfName"
          size="small"
          style="width: 200px"
          placeholder="输入工作流名称"
          @change="onRename"
        />
        <template v-if="current?.id">
          <el-button size="small" :type="isOnline ? 'warning' : 'success'" plain @click="toggleOnline">
            {{ isOnline ? '下线' : '上线' }}
          </el-button>
          <el-button size="small" @click="openVersions">版本</el-button>
        </template>
        <span class="wsw-header-hint mono">{{ current?.id ? '已保存 · ' + current.id.slice(0, 8) : '未保存的新工作流，画布即为编辑入口' }}</span>
      </div>
      <div class="wsw-canvas-wrap">
        <WorkflowCanvas
          v-show="tab === 'design'"
          :key="`${current?.id || 'new'}-${canvasKey}`"
          :workflow="current"
          :name="wfName"
          :readonly="isOnline"
          :focus-node="focusNode"
          @saved="onSaved"
        />
        <div v-show="tab === 'schedule'" class="wsw-panel">
          <SchedulePanel :workflow="current" />
        </div>
        <div v-show="tab === 'instance'" class="wsw-panel">
          <InstancePanel :workflow="current" />
        </div>
        <div v-show="tab === 'monitor'" class="wsw-panel">
          <MonitorPanel :workflow="current" @locate="onLocate" />
        </div>
      </div>
    </section>

    <!-- version drawer -->
    <el-drawer v-model="versionOpen" title="版本历史" size="420px">
      <el-empty v-if="!versions.length" description="暂无历史版本" :image-size="60" />
      <div v-else class="wsw-ver">
        <div v-for="v in versions" :key="v.id" class="wsw-ver-item">
          <div class="wsw-ver-head">
            <el-tag size="small" effect="plain">v{{ v.versionNo }}</el-tag>
            <span class="wsw-ver-name">{{ v.name }}</span>
            <span class="mono muted wsw-ver-time">{{ v.createTime || '—' }}</span>
          </div>
          <div class="wsw-ver-sub">
            <span>{{ nodeCount(v.nodesJson) }} 节点</span>
            <span class="wsw-ver-remark">{{ v.remark || '' }}</span>
            <el-button size="small" text type="warning" :disabled="isOnline" @click="doRollback(v)">回滚</el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusBadge from '@/components/base/StatusBadge.vue'
import WorkflowCanvas from './components/WorkflowCanvas.vue'
import SchedulePanel from './components/SchedulePanel.vue'
import InstancePanel from './components/InstancePanel.vue'
import MonitorPanel from './components/MonitorPanel.vue'
import { workflowApi, type WorkflowRes, type WorkflowVersion } from '@/api/workflow'

const list = ref<WorkflowRes[]>([])
const current = ref<WorkflowRes | null>(null)
const wfName = ref('')
const canvasKey = ref(0)
const loading = ref(false)
const tab = ref<'design' | 'schedule' | 'instance' | 'monitor'>('design')

const isOnline = computed(() => current.value?.status === 'ONLINE')

const versionOpen = ref(false)
const versions = ref<WorkflowVersion[]>([])
const focusNode = ref('')

function onLocate(nodeId: string) {
  focusNode.value = nodeId
  tab.value = 'design' // jump back to the canvas to reveal the node
}

async function loadList() {
  loading.value = true
  try {
    list.value = (await workflowApi.list()) || []
    if (!current.value && list.value.length) {
      current.value = list.value[0]
      wfName.value = list.value[0].name
      canvasKey.value++
    }
  } finally {
    loading.value = false
  }
}

function onSelect(wf: WorkflowRes) {
  current.value = wf
  wfName.value = wf.name
  canvasKey.value++ // remount canvas to load the selected DAG
}

function onNew() {
  current.value = null
  wfName.value = '新建工作流'
  canvasKey.value++ // blank canvas, no id → editor is the entry point
}

function onRename() {
  if (current.value && current.value.id) {
    // rename is persisted together with the next save; just keep the name in sync
    current.value = { ...current.value, name: wfName.value || current.value.name }
  }
}

function onSaved(wf: WorkflowRes) {
  current.value = wf
  wfName.value = wf.name
  loadList()
}

async function onDelete(wf: WorkflowRes) {
  await ElMessageBox.confirm(`确认删除工作流 ${wf.name} ？`, '提示', { type: 'warning' })
  await workflowApi.remove(wf.id)
  ElMessage.success('已删除')
  if (current.value?.id === wf.id) onNew()
  else await loadList()
}

async function toggleOnline() {
  if (!current.value?.id || isOnline.value === undefined) return
  try {
    const res = isOnline.value
      ? await workflowApi.offline(current.value.id)
      : await workflowApi.online(current.value.id)
    current.value = res
    await loadList()
    ElMessage.success(isOnline.value ? '已下线，可编辑' : '已上线，定义锁定')
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
    try { await loadList() } catch { /* noop */ }
  }
}

async function openVersions() {
  if (!current.value?.id) return
  versionOpen.value = true
  versions.value = (await workflowApi.versions(current.value.id)) || []
}

function nodeCount(j: string | undefined): number {
  if (!j) return 0
  try { return (JSON.parse(j) as unknown[]).length } catch { return 0 }
}

async function doRollback(v: WorkflowVersion) {
  if (!current.value?.id || isOnline.value) return
  await ElMessageBox.confirm(`确认回滚到 v${v.versionNo} ？将覆盖当前定义`, '提示', { type: 'warning' })
  try {
    const res = await workflowApi.rollback(current.value.id, v.id)
    current.value = res
    wfName.value = res.name
    canvasKey.value++ // reload canvas from rolled-back DAG
    versionOpen.value = false
    await loadList()
    ElMessage.success('已回滚')
  } catch (e: any) {
    ElMessage.error(e?.message || '回滚失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.wsw { display: flex; height: 100%; }
.wsw-list { width: 280px; border-right: 1px solid var(--el-border-color-lighter); display: flex; flex-direction: column; }
.wsw-list-head { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-bottom: 1px solid var(--el-border-color-lighter); }
.wsw-list-title { font-weight: 600; }
.wsw-list-body { flex: 1; overflow: auto; padding: 8px; }
.wsw-item { border: 1px solid var(--el-border-color-lighter); border-radius: 8px; padding: 8px 10px; margin-bottom: 8px; cursor: pointer; background: #fff; }
.wsw-item:hover { border-color: var(--el-color-primary); }
.wsw-item.active { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.wsw-item-main { display: flex; align-items: center; justify-content: space-between; gap: 6px; }
.wsw-item-name { font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wsw-item-sub { display: flex; align-items: center; gap: 8px; margin-top: 4px; font-size: 12px; color: var(--el-text-color-secondary); }
.wsw-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.wsw-header { display: flex; align-items: center; gap: 10px; padding: 10px 16px; border-bottom: 1px solid var(--el-border-color-lighter); }
.wsw-header-tag { color: var(--el-text-color-secondary); font-size: 13px; }
.wsw-header-hint { flex: 1; text-align: right; font-size: 12px; color: var(--el-text-color-placeholder); }
.wsw-canvas-wrap { flex: 1; min-height: 0; }
.wsw-panel { flex: 1; min-height: 0; overflow: auto; background: #fff; }
.wsw-ver { display: flex; flex-direction: column; gap: 10px; }
.wsw-ver-item { border: 1px solid var(--el-border-color-lighter); border-radius: 8px; padding: 10px; }
.wsw-ver-head { display: flex; align-items: center; gap: 8px; }
.wsw-ver-name { font-weight: 600; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wsw-ver-time { font-size: 12px; }
.wsw-ver-sub { display: flex; align-items: center; gap: 10px; margin-top: 6px; font-size: 12px; color: var(--el-text-color-secondary); }
.wsw-ver-remark { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--el-text-color-placeholder); }
.mono { font-family: var(--td-font-mono); }
.muted { color: var(--el-text-color-secondary); }
</style>