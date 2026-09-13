<template>
  <div class="mon" @click="closeMenu">
    <div class="mon-bar">
      <el-select v-model="instanceId" size="small" style="width: 300px" @change="loadDetail" placeholder="选择实例">
        <el-option v-for="inst in instances" :key="inst.id" :value="inst.id"
          :label="`${short(inst.id)} · ${inst.status} · ${inst.startTime || '—'}`" />
      </el-select>
      <el-button size="small" @click="loadAll">刷新</el-button>
    </div>

    <template v-if="detail">
      <el-divider content-position="left">甘特图（任务并行度 / 耗时）</el-divider>
      <div class="gantt-tool">
        <span class="gantt-tool-label">排序：</span>
        <el-select v-model="sortMode" size="small" style="width: 140px">
          <el-option value="start" label="开始时间" />
          <el-option value="end" label="完成时间" />
          <el-option value="dur" label="耗时（长→短）" />
        </el-select>
        <span class="gantt-tool-label">按状态分组：</span>
        <el-switch v-model="groupByStatus" size="small" />
        <span class="gantt-tool-label">缩放：</span>
        <el-slider v-model="zoomLevel" :min="1" :max="8" :step="1" style="width: 120px" />
        <span class="gantt-tool-label">位置：</span>
        <el-slider v-model="viewOffset" :min="0" :max="100" style="width: 120px" :disabled="zoomLevel <= 1" />
        <span class="gantt-tool-oc" />
        <el-button size="small" @click="exportGanttPNG">导出图片</el-button>
      </div>
      <div class="gantt">
        <!-- time axis -->
        <div class="grow grow-head">
          <span class="gname">节点</span>
          <div class="track track-head">
            <template v-for="(t, i) in ganttAxis" :key="t">
              <span class="tick" :style="{ left: (i / Math.max(ganttAxis.length - 1, 1)) * 100 + '%' }">{{ t }}</span>
            </template>
          </div>
        </div>
        <!-- bars grouped by status (optionally) -->
        <template v-for="g in ganttGroups" :key="g.status || 'all'">
          <div v-if="g.status" class="grp-head">
            <StatusBadge :status="g.status" :text="g.status" />
            <span class="grp-count">{{ g.rows.length }} 节点</span>
          </div>
          <div
            v-for="n in g.rows"
            :key="n.nodeId"
            class="grow"
            :title="'点击定位 / 右键更多'"
            @click="locate(n.nodeId)"
            @contextmenu.prevent="openMenu($event, n.nodeId)"
          >
            <span class="gname">{{ n.name }}</span>
            <div class="track">
              <div class="bar" :class="'s-' + n.status" :style="{ left: n.left + '%', width: n.width + '%' }">
                <span v-if="n.width > 8" class="bar-dur">{{ n.dur }}ms</span>
                <div class="tt">
                  <b>{{ n.name }}</b>
                  <i class="tt-row"><StatusBadge :status="n.status" :text="n.status" :size="'small'" /></i>
                  <span class="tt-row">{{ n.startLabel }} → {{ n.endLabel }}</span>
                  <span class="tt-row">耗时 <b>{{ n.dur }}ms</b></span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <el-divider content-position="left">DAG 节点状态</el-divider>
      <el-table :data="detail.nodes || []" size="small">
        <el-table-column prop="nodeName" label="节点" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始" min-width="140" />
        <el-table-column label="耗时(ms)" width="100">
          <template #default="{ row }"><span class="mono">{{ row.durationMs ?? '—' }}</span></template>
        </el-table-column>
      </el-table>
    </template>

    <el-divider content-position="left">树形依赖与历史成功率（可折叠 / 点击定位）</el-divider>
    <el-tree
      v-if="treeData.length"
      :data="treeData"
      node-key="nodeId"
      default-expand-all
      :props="{ children: 'children', label: 'name' }"
    >
      <template #default="{ data }">
        <span class="mon-tree-item" @click.stop="locate(data.nodeId)">
          <el-tag size="small" effect="plain">{{ data.name }}</el-tag>
          <span class="mon-rates">
            <i :class="{ ok: data.successRate >= 50 }">成功 {{ data.successRate }}%</i>
            <i :class="{ bad: data.failRate > 0 }">失败 {{ data.failRate }}%</i>
          </span>
        </span>
      </template>
    </el-tree>
    <el-empty v-else description="暂无依赖树（无节点或无边）" :image-size="50" />

    <!-- context menu (right click on a gantt row) -->
    <div v-if="menu" class="ctxm" :style="{ left: menu.x + 'px', top: menu.y + 'px' }">
      <div class="ctxm-item" @click="menuLocate">定位到画布节点</div>
      <div class="ctxm-item" @click="menuExport">导出甘特图为 PNG</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { workflowApi, type WorkflowInstanceRes, type WorkflowRes } from '@/api/workflow'

const props = defineProps<{ workflow?: WorkflowRes | null }>()
const emit = defineEmits<{ (e: 'locate', nodeId: string): void }>()

const instances = ref<WorkflowInstanceRes[]>([])
const instanceId = ref('')
const detail = ref<WorkflowInstanceRes | null>(null)
const sortMode = ref<'start' | 'end' | 'dur'>('start')
const groupByStatus = ref(false)
const successCount = ref<Map<string, number>>(new Map())
const totalCount = ref<Map<string, number>>(new Map())

const NODE_COLOR: Record<string, string> = {
  SUCCESS: '#67c23a', FAILED: '#f56c6c', RUNNING: '#409eff', SKIPPED: '#909399', WAITING: '#c0c4cc',
}

function short(id: string): string { return id ? id.slice(0, 10) : '—' }

async function loadAll() {
  if (!props.workflow?.id) return
  instances.value = (await workflowApi.instances(props.workflow.id)) || []
  if (instances.value.length) {
    if (!instanceId.value || !instances.value.some((i) => i.id === instanceId.value)) {
      instanceId.value = instances.value[0].id
    }
    await stats()
    await loadDetail()
  } else {
    detail.value = null
  }
}

async function loadDetail() {
  if (!instanceId.value) { detail.value = null; return }
  detail.value = await workflowApi.instanceDetail(instanceId.value)
}

async function stats() {
  const sc = new Map<string, number>()
  const tc = new Map<string, number>()
  const latest = instances.value.slice(0, 5)
  for (const inst of latest) {
    try {
      const det = await workflowApi.instanceDetail(inst.id)
      det.nodes?.forEach((n) => {
        tc.set(n.nodeId, (tc.get(n.nodeId) || 0) + 1)
        if (n.status === 'SUCCESS') sc.set(n.nodeId, (sc.get(n.nodeId) || 0) + 1)
      })
    } catch { /* skip */ }
  }
  successCount.value = sc
  totalCount.value = tc
}

const rawRows = computed(() => {
  const nodes = detail.value?.nodes || []
  return nodes.map((n) => {
    const s = new Date(n.startTime || '').getTime()
    const d = n.durationMs || 1
    return { nodeId: n.nodeId, name: n.nodeName, status: n.status, start: Number.isNaN(s) ? 0 : s, dur: d, startLabel: n.startTime || '—' }
  })
})

// gantt zoom / viewport (window of the shown time range)
const zoomLevel = ref(1)
const viewOffset = ref(0)
const ganttWindow = computed(() => {
  const rows = rawRows.value
  if (!rows.length) return { min: 0, span: 1 }
  const gmin = Math.min(...rows.map((r) => r.start))
  const gmax = Math.max(...rows.map((r) => r.start + r.dur))
  const gspan = Math.max(gmax - gmin, 1)
  const span = gspan / Math.max(zoomLevel.value, 1)
  const room = gspan - span
  const off = Math.min(Math.max((halfUse(viewOffset.value) / 100) * room, 0), room)
  return { min: gmin + off, span: Math.max(span, 1) }
})
function halfUse(v: number) { const c = Math.max(0, Math.min(100, v)); return zoomLevel.value > 1 ? c : 0 }

const ganttRows = computed(() => {
  const w = ganttWindow.value
  const fmt = (t: number) =>
    new Date(t).toLocaleTimeString('zh-CN', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' })
  const rows = rawRows.value.map((r) => ({
    ...r,
    left: ((r.start - w.min) / w.span) * 100,
    width: (r.dur / w.span) * 100,
    endLabel: fmt(r.start + r.dur),
  }))
  // sort: start asc / end asc / duration desc
  const cmp =
    sortMode.value === 'end' ? (a: typeof rows[0], b: typeof rows[0]) => a.start + a.dur - (b.start + b.dur)
      : sortMode.value === 'dur' ? (a: typeof rows[0], b: typeof rows[0]) => b.dur - a.dur
        : (a: typeof rows[0], b: typeof rows[0]) => a.start - b.start
  rows.sort(cmp)
  return rows
})

// time axis ticks (evenly spaced) over the zoom window
const ganttAxis = computed(() => {
  const w = ganttWindow.value
  const fmt = (t: number) =>
    new Date(t).toLocaleTimeString('zh-CN', { hour12: false, hour: '2-digit', minute: '2-digit' })
  const ticks = 6
  const out: string[] = []
  for (let i = 0; i <= ticks; i++) out.push(fmt(w.min + (w.span / ticks) * i))
  return [...new Set(out)]
})

// bars optionally grouped into status blocks (group keeps the chosen order)
const ganttGroups = computed<{ status: string; rows: any[] }[]>(() => {
  const rows = ganttRows.value as any[]
  if (!groupByStatus.value) return [{ status: '', rows }]
  const order = ['FAILED', 'RUNNING', 'WAITING', 'SUCCESS', 'SKIPPED']
  const m = new Map<string, any[]>(order.map((s) => [s, []]))
  const rest: any[] = []
  rows.forEach((r) => {
    if (m.has(r.status)) m.get(r.status)!.push(r)
    else rest.push(r)
  })
  const groups: { status: string; rows: any[] }[] = order
    .filter((s) => (m.get(s) || []).length)
    .map((s) => ({ status: s, rows: m.get(s) || [] }))
  if (rest.length) groups.push({ status: '', rows: rest })
  return groups
})

function locate(nodeId: string) {
  emit('locate', nodeId)
}

// ---- context menu ----
const menu = ref<{ x: number; y: number; nodeId: string } | null>(null)
function openMenu(e: MouseEvent, nodeId: string) {
  menu.value = { x: e.clientX, y: e.clientY, nodeId }
}
function closeMenu() {
  menu.value = null
}
function menuLocate() {
  if (menu.value) emit('locate', menu.value.nodeId)
  closeMenu()
}
function menuExport() {
  closeMenu()
  exportGanttPNG()
}

const STATUS_COLOR: Record<string, string> = {
  SUCCESS: '#4caf50', FAILED: '#e53935', RUNNING: '#2196f3', SKIPPED: '#8e959c', WAITING: '#b0b6bd',
}

function exportGanttPNG() {
  const rows = ganttRows.value as any[]
  if (!rows.length) { return }
  const pad = 44
  const rowH = 26
  const labelW = 190
  const trackW = 600
  const W = labelW + trackW + pad * 2
  const H = pad + rows.length * rowH + 12
  const cv = document.createElement('canvas')
  cv.width = W
  cv.height = H
  const ctx = cv.getContext('2d')!
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, W, H)
  const min = Math.min(...rows.map((r) => r.start))
  const max = Math.max(...rows.map((r) => r.start + r.dur))
  const span = Math.max(max - min, 1)
  // gridlines
  ctx.strokeStyle = '#e5e7eb'
  for (let i = 0; i <= 6; i++) {
    const x = pad + labelW + (i / 6) * trackW
    ctx.beginPath(); ctx.moveTo(x, pad - 12); ctx.lineTo(x, H - 12); ctx.stroke()
  }
  const fmt = (t: number) => new Date(t).toLocaleTimeString('zh-CN', { hour12: false, hour: '2-digit', minute: '2-digit' })
  ctx.fillStyle = '#6b7280'
  ctx.font = '11px sans-serif'
  for (let i = 0; i <= 6; i++) ctx.fillText(fmt(min + (span / 6) * i), pad + labelW + (i / 6) * trackW - 12, pad - 4)
  rows.forEach((r, i) => {
    const y = pad + i * rowH
    ctx.fillStyle = '#1f2937'
    ctx.font = '12px sans-serif'
    ctx.textBaseline = 'middle'
    ctx.fillText(r.name, pad, y + rowH / 2)
    const x = pad + labelW + ((r.start - min) / span) * trackW
    const w = Math.max((r.dur / span) * trackW, 4)
    ctx.fillStyle = STATUS_COLOR[r.status] || '#9ca3af'
    ctx.fillRect(x, y + 5, w, rowH - 10)
  })
  const url = cv.toDataURL('image/png')
  const a = document.createElement('a')
  a.href = url
  a.download = `gantt-${detail.value?.id?.slice(0, 8) || 'workflow'}.png`
  a.click()
}

// recursive dependency tree (roots = nodes with no incoming edges) + per-node success rate
const treeData = computed(() => {
  const wf = props.workflow
  const edges = wf?.edges || []
  const nodes = wf?.nodes || []
  const byId = new Map(nodes.map((n) => [n.id, n]))
  const children = new Map(nodes.map((n) => [n.id, [] as string[]]))
  const indeg = new Map(nodes.map((n) => [n.id, 0]))
  edges.forEach((e) => {
    if (!children.has(e.sourceNodeId) || !children.has(e.targetNodeId)) return
    children.get(e.sourceNodeId)!.push(e.targetNodeId)
    indeg.set(e.targetNodeId, indeg.get(e.targetNodeId)! + 1)
  })
  const roots = nodes.filter((n) => indeg.get(n.id) === 0).map((n) => n.id)
  const visited = new Set<string>()
  const build = (id: string): any => {
    visited.add(id)
    const total = totalCount.value.get(id) || 0
    const ok = successCount.value.get(id) || 0
    const rate = total ? Math.round((ok / total) * 100) : 0
    return {
      nodeId: id,
      name: byId.get(id)?.name || id,
      successRate: rate,
      failRate: total ? 100 - rate : 0,
      children: (children.get(id) || []).filter((c) => !visited.has(c)).map(build),
    }
  }
  return roots.map(build)
})

watch(
  () => props.workflow?.id,
  (id) => { if (id) loadAll() },
  { immediate: false },
)
void NODE_COLOR
</script>

<style scoped>
.mon { padding: 16px; max-width: 1200px; }
.mon-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.mon-gantt { display: flex; flex-direction: column; gap: 6px; }
.gantt-tool-oc { flex: 1; }
.ctxm {
  position: fixed; z-index: 100; min-width: 150px; background: #fff; border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px; box-shadow: 0 4px 14px rgba(0, 0, 0, 0.16); padding: 4px;
}
.ctxm-item { padding: 7px 10px; font-size: 13px; border-radius: 4px; cursor: pointer; }
.ctxm-item:hover { background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
.gantt-tool { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.gantt-tool-label { font-size: 12px; color: var(--el-text-color-secondary); }
.gantt { border: 1px solid var(--el-border-color-lighter); border-radius: 8px; overflow: hidden; }
.grow { display: flex; align-items: center; gap: 10px; padding: 2px 10px; border-bottom: 1px solid var(--el-border-color-lighter); }
.grow:nth-child(odd) { background: #fafcff; }
.grow:last-child { border-bottom: none; }
.grow-head { background: var(--el-fill-color-light) !important; font-weight: 600; color: var(--el-text-color-secondary); }
.grp-head {
  display: flex; align-items: center; gap: 8px; padding: 6px 10px;
  background: var(--el-fill-color-lighter); border-bottom: 1px solid var(--el-border-color-lighter);
}
.grp-count { font-size: 12px; color: var(--el-text-color-secondary); }
.grow { cursor: pointer; }
.gname {
  width: 170px; font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  flex: none;
}
.track { flex: 1; height: 26px; position: relative; overflow: hidden; background: repeating-linear-gradient(90deg, transparent 0 calc(10% - 1px), var(--el-border-color-lighter) calc(10% - 1px) 10%); }
.track-head { height: 22px; }
.tick {
  position: absolute; top: 0; transform: translateX(-50%); font-size: 11px;
  color: var(--el-text-color-secondary); white-space: nowrap;
}
.bar {
  position: absolute; top: 5px; height: 16px; border-radius: 8px; min-width: 6px;
  box-shadow: 0 1px 1px rgba(0, 0, 0, 0.12); border: 1px solid rgba(0, 0, 0, 0.08);
  overflow: visible;
}
.bar-dur { position: absolute; left: 8px; top: 0; line-height: 15px; font-size: 11px; color: rgba(255, 255, 255, 0.95); text-shadow: 0 1px 1px rgba(0, 0, 0, 0.3); white-space: nowrap; }
/* per-status gradient bars */
.s-SUCCESS { background: linear-gradient(180deg, #7fd08a, #4caf50); }
.s-SUCCESS:hover { filter: brightness(1.08); }
.s-FAILED { background: linear-gradient(180deg, #ef8b8b, #e53935); }
.s-RUNNING { background: linear-gradient(180deg, #6bb4ff, #2196f3); animation: run-pulse 1.2s ease-in-out infinite; }
.s-SKIPPED { background: linear-gradient(180deg, #aab0b8, #8e959c); }
.s-WAITING { background: repeating-linear-gradient(45deg, #cfd6dd 0 6px, #c3cad2 6px 12px); }
@keyframes run-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.75; } }
/* hover tooltip */
.bar .tt {
  display: none; position: absolute; bottom: calc(100% + 6px); left: 50%; transform: translateX(-50%);
  background: #303133; color: #fff; border-radius: 6px; padding: 6px 9px; font-size: 12px; line-height: 1.5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2); z-index: 30; width: max-content; max-width: 220px;
}
.bar .tt b { color: #fff; }
.bar .tt .tt-row { display: block; color: #d9dce0; }
.bar:hover .tt { display: block; }
.mon-tree { display: flex; gap: 20px; overflow-x: auto; padding-bottom: 8px; }
.mon-tree-item { display: inline-flex; align-items: center; gap: 10px; font-size: 13px; cursor: pointer; }
.mon-rates { display: flex; gap: 8px; font-size: 12px; color: var(--el-text-color-secondary); }
.mon-rates i.ok { color: #67c23a; font-style: normal; }
.mon-rates i.bad { color: #f56c6c; font-style: normal; }
.mono { font-family: var(--td-font-mono); }
</style>