<template>
  <div ref="containerRef" class="lineage-g6-container">
    <div class="canvas-actions">
      <button class="ca-btn" title="放大" @click="zoomBy(1.25)">+</button>
      <button class="ca-btn" title="缩小" @click="zoomBy(0.8)">−</button>
      <div class="ca-sep"></div>
      <button class="ca-btn" title="适应画布" @click="fitView">⤢</button>
      <button class="ca-btn" title="重置缩放 100%" @click="resetView">1:1</button>
      <div class="ca-sep"></div>
      <button class="ca-btn" :class="{ active: minimapVisible }" title="小地图" @click="toggleMinimap">🗺</button>
      <button class="ca-btn" :class="{ active: legendVisible }" title="图例" @click="toggleLegend">ℹ</button>
      <div class="ca-sep"></div>
      <button class="ca-btn" title="Fullscreen" @click="doFullscreen">⛶</button>
      <button class="ca-btn" :class="{ active: props.screenshotLock }" :title="props.screenshotLock ? 'Screenshot lock ON (includes legend & minimap)' : 'Screenshot lock OFF'" @click="emit('update:screenshotLock', !props.screenshotLock)">📷</button>
    </div>

    <!-- 自定义小地图 -->
    <div v-show="minimapVisible" class="lineage-minimap">
      <canvas ref="minimapCanvasRef" width="180" height="120"></canvas>
    </div>

    <!-- 图例 -->
    <div v-if="legendVisible" class="legend-panel">
      <div class="legend-title">图例</div>
      <div class="legend-section">
        <div class="legend-sub">节点类型</div>
        <div class="legend-item" v-for="n in legendNodes" :key="n.label">
          <span class="legend-swatch" :style="{ background: n.color }"></span>
          <span class="legend-label">{{ n.label }}</span>
        </div>
      </div>
      <div class="legend-section">
        <div class="legend-sub">边类型</div>
        <div class="legend-item" v-for="e in legendEdges" :key="e.label">
          <span class="legend-line" :style="{ borderColor: e.color, borderStyle: e.dash ? 'dashed' : 'solid' }"></span>
          <span class="legend-label">{{ e.label }}</span>
        </div>
      </div>
    </div>

    <!-- 边详情浮层 -->
    <div v-if="edgeInfo.visible" class="edge-info" :style="{ left: edgeInfo.x + 'px', top: edgeInfo.y + 'px' }">
      <div class="edge-info-head">
        <el-tag size="small" :type="edgeInfo.tagType" effect="dark">{{ edgeInfo.relationLabel }}</el-tag>
        <button class="edge-info-close" @click="edgeInfo.visible = false">×</button>
      </div>
      <div class="edge-info-body">
        <div class="edge-info-row"><span class="ei-k">来源</span><span class="ei-v">{{ edgeInfo.sourceName }}</span></div>
        <div class="edge-info-row"><span class="ei-k">目标</span><span class="ei-v">{{ edgeInfo.targetName }}</span></div>
        <div v-if="edgeInfo.taskName" class="edge-info-row"><span class="ei-k">任务</span><span class="ei-v">{{ edgeInfo.taskName }}</span></div>
        <div v-if="edgeInfo.sql" class="edge-info-row"><span class="ei-k">SQL</span><span class="ei-v ei-sql">{{ edgeInfo.sql }}</span></div>
        <div v-if="edgeInfo.fieldLinks && edgeInfo.fieldLinks.length" class="edge-info-fields">
          <div class="edge-info-sub">字段映射</div>
          <div v-for="(f, i) in edgeInfo.fieldLinks.slice(0, 8)" :key="i" class="edge-info-field">
            <span class="eif-s">{{ f.sourceColumn }}</span> → <span class="eif-t">{{ f.targetColumn }}</span>
          </div>
        </div>
        <div class="edge-info-actions">
          <button class="ei-btn" @click.stop="emit('edge-edit', selectedEdgeData)" :disabled="!props.canWrite" :aria-disabled="!props.canWrite" :title="props.canWrite ? 'Edit this edge' : 'Missing LINEAGE:WRITE permission'">Edit</button>
          <button class="ei-btn ei-btn-danger" @click.stop="emit('edge-delete', selectedEdgeData)" :disabled="!props.canWrite" :aria-disabled="!props.canWrite" :title="props.canWrite ? 'Delete this edge' : 'Missing LINEAGE:WRITE permission'">Delete</button>
        </div>
      </div>
    </div>

    <div v-if="contextMenu.visible" class="ctx-menu" :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }">
      <div v-if="contextMenu.isAgg" class="ctx-item" @click="ctxExpand">展开该层节点</div>
      <template v-else>
        <div class="ctx-item" :class="{ disabled: !props.canWrite }" role="menuitem" :aria-disabled="!props.canWrite" :title="props.canWrite ? 'Add upstream relationship' : 'Missing LINEAGE:WRITE permission'" @click="props.canWrite && emit('ctx-add-upstream', { nodeId: contextMenu.node?.id, node: contextMenu.node })">
          ⇠ Add upstream link
        </div>
        <div class="ctx-item" :class="{ disabled: !props.canWrite }" role="menuitem" :aria-disabled="!props.canWrite" :title="props.canWrite ? 'Add downstream relationship' : 'Missing LINEAGE:WRITE permission'" @click="props.canWrite && emit('ctx-add-downstream', { nodeId: contextMenu.node?.id, node: contextMenu.node })">
          Add downstream link ⇢
        </div>
        <div class="ctx-item" @click="ctxDetail">查看详情</div>
        <div class="ctx-item" @click="ctxRefocus">以此节点为中心</div>
      </template>
    </div>
    <div v-show="tooltip.visible" class="lineage-tooltip" :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }">
      <div class="lt-head">
        <span class="lt-type">{{ tooltip.typeTag }}</span>
        <span class="lt-name">{{ tooltip.name }}</span>
      </div>
      <div class="lt-row" v-if="tooltip.datasourceName"><span class="lt-k">数据源</span><span class="lt-v">{{ tooltip.datasourceName }}</span></div>
      <div class="lt-row" v-if="tooltip.relations.length"><span class="lt-k">关联任务</span><span class="lt-v">{{ tooltip.relations.join('、') }}</span></div>
      <div class="lt-row" v-if="tooltip.sql"><span class="lt-k">转换 SQL</span><span class="lt-v lt-sql">{{ tooltip.sql }}</span></div>
      <div class="lt-divider"></div>
      <div class="lt-row"><span class="lt-k">层级</span><span class="lt-v">L{{ tooltip.layer }}</span></div>
      <div class="lt-row"><span class="lt-k">热度</span><span class="lt-v">↑{{ tooltip.deg }} 条连线</span></div>
      <div class="lt-row" v-if="tooltip.rowCount != null"><span class="lt-k">行数</span><span class="lt-v">{{ tooltip.rowCount }}</span></div>
      <div class="lt-row" v-if="tooltip.columnCount != null"><span class="lt-k">列数</span><span class="lt-v">{{ tooltip.columnCount }}</span></div>
      <div class="lt-row" v-if="tooltip.category"><span class="lt-k">位置</span><span class="lt-v">{{ tooltip.category === 'root' ? '根节点' : tooltip.category === 'upstream' ? '上游' : tooltip.category === 'downstream' ? '下游' : tooltip.category }}</span></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, onBeforeUnmount, shallowRef, nextTick } from 'vue'
import { Graph } from '@antv/g6'

const props = defineProps({
  data: { type: Object, default: null },
  layout: { type: String, default: 'layer' },
  cycleEdgeKeys: { type: Array, default: () => [] },
  // Which layers to render (entity / column / dq). Parent drives via Chip toggles.
  layers: { type: Object, default: () => ({ entity: true, column: false, dq: false }) },
  // Per-node session-level x/y overrides (drag to reposition, cleared on layout change).
  layoutOverrides: { type: Object, default: () => ({}) },
  // Node ID -> DQ score (0..100) lookup map. If missing, badge falls back to "?" gray tooltip.
  dqScoreMap: { type: Object, default: () => ({}) },
  // List of edge IDs that must render with the purple "col-highlight" state.
  highlightedFieldEdgeIds: { type: Array, default: () => [] },
  // When true, exportPng() composits legend & minimap on top of the graph canvas.
  screenshotLock: { type: Boolean, default: false },
  // Used by radial layout to set the center anchor node.
  centerNodeId: { type: String, default: '' },
  // When false, write actions (edit/delete/add link) are disabled with tooltip hint.
  canWrite: { type: Boolean, default: true },
  // Sorted array of node IDs that should be kept visible when the filter is active. All other nodes/edges (except those NOT in a "kept" relationship with them) should receive dim state.
  filterKeepIds: { type: Array, default: () => [] },
  // Enable filter-dim state application. When true and filterKeepIds is non-empty, nodes not in keepIds + edges whose (source AND target) not both in keepIds are applied `filter-dim` state (opacity 0.15).
  filterDimEnabled: { type: Boolean, default: false }
})

const emit = defineEmits(['node-click', 'node-dblclick', 'edge-edit', 'edge-delete', 'ctx-add-upstream', 'ctx-add-downstream', 'node-dragend', 'update:screenshotLock'])

const containerRef = ref(null)
const graphRef = shallowRef(null)
const minimapCanvasRef = ref(null)
const rawNodes = ref([])
const rawEdges = ref([])
const expandedLayers = ref(new Set())
const minimapVisible = ref(true)
const legendVisible = ref(false)
const edgeInfo = reactive({ visible: false, x: 0, y: 0, sourceName: '', targetName: '', taskName: '', sql: '', fieldLinks: [], relationLabel: '', tagType: '' })
const selectedEdgeData = shallowRef(null)
const tooltip = reactive({
  visible: false, x: 0, y: 0, name: '', typeTag: '', datasourceName: '',
  relations: [], sql: '', layer: 0, deg: 0, rowCount: null, columnCount: null, category: ''
})
const contextMenu = reactive({ visible: false, x: 0, y: 0, node: null, isAgg: false })
let animFrame = null
let flowOffset = 0
let clickTimer = null
let minimapPlugin = null
let minimapDragging = false
let minimapDragOffsetX = 0
let minimapDragOffsetY = 0
let minimapPointerMoveHandler = null
let windowPointerUpHandler = null
let minimapWheelHandler = null

const AGG_THRESHOLD = 50

const legendNodes = [
  { label: '根节点', color: '#fbbf24' },
  { label: '数据表', color: '#22d3ee' },
  { label: '字段', color: '#c4b5fd' },
  { label: '任务', color: '#fb7185' },
  { label: '数据源', color: '#fb923c' },
  { label: '报表', color: '#a78bfa' },
  { label: '聚合节点', color: '#64748b' }
]

const legendEdges = [
  { label: '数据流转 FLOWS_TO', color: '#10b981', dash: false },
  { label: '衍生 DERIVED_FROM', color: '#a78bfa', dash: false },
  { label: '产出 PRODUCES', color: '#f59e0b', dash: false },
  { label: '消费 CONSUMES', color: '#38bdf8', dash: false }
]

function edgeRelationLabel(t) {
  return t === 'FLOWS_TO' ? '数据流转' : t === 'DERIVED_FROM' ? '衍生' : t === 'PRODUCES' ? '产出' : t === 'CONSUMES' ? '消费' : (t || '关联')
}

function edgeTagType(t) {
  return t === 'FLOWS_TO' ? 'success' : t === 'DERIVED_FROM' ? 'warning' : t === 'PRODUCES' ? 'danger' : t === 'CONSUMES' ? 'primary' : 'info'
}

function toggleMinimap() {
  minimapVisible.value = !minimapVisible.value
  if (minimapVisible.value) drawMinimap()
}

// Minimap continuous sync: rAF-throttled redraw keeps pace during zoom / pan / layout animation.
let minimapTick = null
function startMinimapTicker() {
  stopMinimapTicker()
  const tick = () => {
    if (minimapVisible.value) {
      drawMinimap()
      minimapTick = setTimeout(tick, 300)
    } else {
      minimapTick = null
    }
  }
  minimapTick = setTimeout(tick, 300)
}
function stopMinimapTicker() {
  if (minimapTick) {
    clearTimeout(minimapTick)
    minimapTick = null
  }
}

function minimapColor(d) {
  if (d.nodeType === 'AGG') return '#64748B';
  if (d.category === 'root') return '#FBBF24';
  if (d.category === 'upstream') return '#10B981';
  if (d.category === 'downstream') return '#38BDF8';
  return '#94A3B8'; // default neutral
}

function drawMinimap() {
  const graph = graphRef.value
  const canvas = minimapCanvasRef.value
  if (!graph || !canvas) return
  try {
    const W = 180, H = 120, pad = 8
    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, W, H)
    // White background
    ctx.fillStyle = '#FFFFFF'
    ctx.fillRect(0, 0, W, H)
    // Outline rectangle
    ctx.strokeStyle = '#E0E0E0'
    ctx.lineWidth = 1
    ctx.strokeRect(0.5, 0.5, W - 1, H - 1)
    const nodes = graph.getNodeData()
    if (!nodes.length) return
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
    for (const n of nodes) {
      const p = graph.getElementPosition(n.id)
      if (!p) continue
      if (p[0] < minX) minX = p[0]
      if (p[1] < minY) minY = p[1]
      if (p[0] > maxX) maxX = p[0]
      if (p[1] > maxY) maxY = p[1]
    }
    if (maxX <= minX) maxX = minX + 1
    if (maxY <= minY) maxY = minY + 1
    const scale = Math.min((W - pad * 2) / (maxX - minX), (H - pad * 2) / (maxY - minY))
    const ox = (W - (maxX - minX) * scale) / 2 - minX * scale
    const oy = (H - (maxY - minY) * scale) / 2 - minY * scale
    // edges
    ctx.strokeStyle = 'rgba(52, 211, 153, 0.35)'
    ctx.lineWidth = 1
    for (const e of graph.getEdgeData()) {
      const s = graph.getElementPosition(e.source)
      const t = graph.getElementPosition(e.target)
      if (!s || !t) continue
      ctx.beginPath()
      ctx.moveTo(s[0] * scale + ox, s[1] * scale + oy)
      ctx.lineTo(t[0] * scale + ox, t[1] * scale + oy)
      ctx.stroke()
    }
    // nodes with category/nodeType coloring
    for (const n of nodes) {
      const p = graph.getElementPosition(n.id)
      if (!p) continue
      const nodeData = n.data || {}
      ctx.fillStyle = minimapColor(nodeData)
      ctx.beginPath()
      ctx.arc(p[0] * scale + ox, p[1] * scale + oy, 2, 0, Math.PI * 2)
      ctx.fill()
    }
    // viewport rect — brand blue
    try {
      const zoom = graph.getZoom()
      const vw = containerRef.value?.clientWidth || 0
      const vh = containerRef.value?.clientHeight || 0
      // Canvas coordinates that correspond to the viewport center (viewport center = [vw/2, vh/2]).
      const vc = graph.getCanvasByViewport([vw / 2, vh / 2])
      const w = vw / zoom * scale
      const h = vh / zoom * scale
      const cx = vc[0] * scale + ox - w / 2
      const cy = vc[1] * scale + oy - h / 2
      ctx.strokeStyle = '#4A90E2'
      ctx.lineWidth = 2
      ctx.strokeRect(cx, cy, w, h)
    } catch (e) { /* ignore */ }
  } catch (e) { /* ignore */ }
}

function toggleLegend() {
  legendVisible.value = !legendVisible.value
}

function nodePalette(d) {
  // Light card theme with colored accents (white card + colored accent bar, matches the design reference).
  if (d.nodeType === 'AGG') return { fill: '#eef2ff', stroke: '#6366f1' }
  if (d.nodeType === 'COLUMN') return { fill: '#f3e8ff', stroke: '#a855f7' }
  if (d.nodeType === 'TASK') return { fill: '#fff1f2', stroke: '#f43f5e' }
  if (d.nodeType === 'REPORT') return { fill: '#f5f3ff', stroke: '#8b5cf6' }
  if (d.nodeType === 'DATA_SOURCE') return { fill: '#fff7ed', stroke: '#f97316' }
  if (d.category === 'root') return { fill: '#e0f2fe', stroke: '#0ea5e9' }
  if (d.category === 'upstream') return { fill: '#ecfdf5', stroke: '#10b981' }
  return { fill: '#eff6ff', stroke: '#3b82f6' }
}

function nodeShape(d) {
  if (d.nodeType === 'AGG') return 'diamond'
  return 'rect'
}

function degreeMap() {
  const map = {}
  for (const e of rawEdges.value) {
    map[e.source] = (map[e.source] || 0) + 1
    map[e.target] = (map[e.target] || 0) + 1
  }
  return map
}

function nodeSize(d) {
  if (d.nodeType === 'AGG') return [96, 44]
  if (d.nodeType === 'COLUMN') return [Math.max(64, (d.name || '').length * 7 + 26), 24]
  const deg = degreeMap()[d.id] || 0
  const w = Math.min(220, Math.max(150, (d.name || '').length * 9 + 36 + Math.min(deg * 5, 40)))
  return [w, 52]
}

function typeTag(n) {
  if (n.nodeType === 'TASK') return 'JOB'
  if (n.nodeType === 'TABLE' || n.nodeType === 'DATA_SOURCE') return 'DATASET'
  if (n.nodeType === 'COLUMN') return 'FIELD'
  if (n.nodeType === 'AGG') return 'AGG'
  return n.nodeType || 'NODE'
}

function subLabel(d) {
  if (d.nodeType === 'AGG') return `${d.aggregatedCount ?? ''}节点聚合`
  if (d.nodeType === 'COLUMN') return ''
  const deg = degreeMap()[d.id] || 0
  const rows = d.rowCount != null ? ` · ${d.rowCount}行` : ''
  const base = `${typeTag(d)} · L${d.layer ?? 0} · ↑${deg}${rows}`
  const score = props.dqScoreMap[d.id]
  const dqTag = score != null ? ` · DQ:${score}` : ''
  return base + dqTag
}

function buildData() {
  const degrees = degreeMap()

  // --- P7: Layers rendering modes ---
  let nodesSrc = [...rawNodes.value]
  // Entity-layer filter: when entity is disabled, keep only COLUMN and synthetic AGG nodes
  if (!props.layers.entity) {
    nodesSrc = nodesSrc.filter(n => n.nodeType === 'COLUMN' || n.nodeType === 'AGG')
  }
  // Synthetic column expansion: when column layer is on AND entity is off AND graph is small
  const synthEdges = []
  if (props.layers.column && !props.layers.entity && rawNodes.value.length + rawEdges.value.length > 0 && rawNodes.value.length <= 200) {
    const colNodes = new Map()
    for (const e of rawEdges.value) {
      if (!e.fieldLinks || !e.fieldLinks.length) continue
      for (const fl of e.fieldLinks) {
        const srcColId = `${e.source}.${fl.sourceColumn}`
        const tgtColId = `${e.target}.${fl.targetColumn}`
        if (!colNodes.has(srcColId)) {
          colNodes.set(srcColId, {
            id: srcColId,
            name: fl.sourceColumn,
            nodeType: 'COLUMN',
            category: 'column',
            layer: 0,
            ownerTableId: e.source
          })
        }
        if (!colNodes.has(tgtColId)) {
          colNodes.set(tgtColId, {
            id: tgtColId,
            name: fl.targetColumn,
            nodeType: 'COLUMN',
            category: 'column',
            layer: 0,
            ownerTableId: e.target
          })
        }
        synthEdges.push({
          id: `col:${srcColId}__${tgtColId}`,
          source: srcColId,
          target: tgtColId,
          relationType: 'COLUMN_FLOW',
          taskName: '',
          sql: '',
          fieldLinks: [],
          synthetic: true,
          columnEdge: true
        })
      }
    }
    nodesSrc = [...nodesSrc, ...colNodes.values()]
  } else if (props.layers.column && !props.layers.entity && rawNodes.value.length > 200) {
    console.warn('[LineageGraph] Skipping synthetic column expansion: raw node count exceeds 200 threshold')
  }

  let nodes = nodesSrc
  let edges = [...rawEdges.value, ...synthEdges]
  const isAggregated = rawNodes.value.length > AGG_THRESHOLD

  // Node aggregation (+N) when the graph exceeds the threshold
  if (rawNodes.value.length > AGG_THRESHOLD) {
    const byLayer = {}
    for (const n of nodesSrc) {
      const layer = n.layer ?? 0
      ;(byLayer[layer] = byLayer[layer] || []).push(n)
    }
    const aggregated = {}
    const realNodes = []
    for (const [layer, list] of Object.entries(byLayer)) {
      if (Number(layer) === 0 || expandedLayers.value.has(Number(layer)) || list.length <= 3) {
        realNodes.push(...list)
      } else {
        const aggId = 'AGG:L' + layer
        aggregated[aggId] = list.map(n => n.id)
        realNodes.push({
          id: aggId,
          name: `L${layer} · ${list.length} 节点`,
          nodeType: 'AGG',
          category: 'aggregate',
          layer: Number(layer),
          aggregatedCount: list.length,
          aggregated: true
        })
      }
    }
    nodes = realNodes
    edges = edges.map(e => {
      const srcAgg = Object.keys(aggregated).find(k => aggregated[k].includes(e.source))
      const tgtAgg = Object.keys(aggregated).find(k => aggregated[k].includes(e.target))
      if (!srcAgg && !tgtAgg) return e
      return {
        ...e,
        source: srcAgg || e.source,
        target: tgtAgg || e.target,
        data: { ...(e.data || {}), aggregated: true }
      }
    })
  }

  return {
    nodes: nodes.map(n => {
      const pal = nodePalette(n)
      const size = nodeSize(n)
      const deg = degrees[n.id] || 0
      const isCard = !['AGG', 'COLUMN'].includes(n.nodeType)
      const isCol = n.nodeType === 'COLUMN'
      const sub = subLabel(n)
      const styleObj = {
        size,
        shape: nodeShape(n),
        radius: isCol ? 3 : (isCard ? 6 : 4),
        fill: pal.fill,
        stroke: pal.stroke,
        lineWidth: isCol ? 1.5 : 2.2,
        shadowColor: 'rgba(0,0,0,0.35)',
        shadowBlur: isCard ? 5 : 2,
        labelText: isCol ? n.name : (sub ? `${n.name}\n${sub}` : n.name),
        labelFill: n.nodeType === 'AGG' ? '#3730a3' : (isCol ? '#4b5563' : '#1f2937'),
        labelFontSize: isCol ? 10.5 : (n.nodeType === 'AGG' ? 11.5 : 11.5),
        labelFontFamily: 'Menlo, Consolas, monospace',
        labelFontWeight: isCard ? 700 : 400,
        labelPosition: 'center',
        labelPlacement: 'center',
        labelBackground: isCard,
        labelBackgroundFill: 'rgba(255,255,255,0.85)',
        labelBackgroundRadius: 4
      }
      // --- P5c: Apply per-node x/y session-level drag overrides ---
      const result = {
        id: n.id,
        data: { ...n },
        style: styleObj
      }
      const ov = props.layoutOverrides[n.id]
      if (ov && ov.x != null && ov.y != null) {
        styleObj.x = ov.x
        styleObj.y = ov.y
        result.position = [ov.x, ov.y]
      }
      return result
    }),
    edges: edges.map((e, i) => {
      const isCycle = props.cycleEdgeKeys.includes(`${e.source}|${e.target}`)
      const mainFlow = e.relationType === 'FLOWS_TO'
      const isSynthCol = e.columnEdge === true
      return {
        id: e.id || 'e' + i,
        source: e.source,
        target: e.target,
        data: { ...e },
        style: {
          stroke: isSynthCol ? '#8B5CF6' : (isCycle ? '#ef4444' : edgeColor(e.relationType)),
          lineWidth: isCycle ? 3.5 : (mainFlow ? (isAggregated ? 2.5 : 3) : (isSynthCol ? 1.5 : 2)),
          lineCap: 'round',
          opacity: 1,
          endArrow: !isSynthCol,
          endArrowSize: mainFlow ? 12 : 10,
          lineDash: isSynthCol ? [6, 4] : undefined,
          // Hide secondary edge labels in aggregated large-graph mode to avoid dense text overlap.
          labelText: isAggregated || e.relationType === 'FLOWS_TO' ? '' : (isSynthCol ? '' : edgeRelationLabel(e.relationType)),
          labelFill: '#334155',
          labelFontSize: 9.5,
          labelBackground: true,
          labelBackgroundFill: 'rgba(255,255,255,0.9)',
          labelBackgroundRadius: 3
        }
      }
    })
  }
}

function edgeColor(t) {
  // Light theme: default gray-blue, active brand blue
  return t === 'FLOWS_TO' ? '#94a3b8'
    : t === 'DERIVED_FROM' ? '#a78bfa'
    : t === 'PRODUCES' ? '#f59e0b'
    : t === 'CONSUMES' ? '#38bdf8'
    : '#b0bec5'
}

function layoutOption() {
  if (props.layout === 'force') {
    return {
      type: 'force',
      linkDistance: 150,
      nodeStrength: -200,
      preventOverlap: true,
      collisionStrength: 5
    }
  }
  if (props.layout === 'radial') {
    return {
      type: 'radial',
      unitRadius: 160,
      linkDistance: 200,
      nodeSpacing: 24,
      centerNodeId: props.centerNodeId || undefined
    }
  }
  // Dagre (layer) branch with node-count threshold tuning
  const base = { type: 'dagre', rankdir: 'LR', nodesep: 32, ranksep: 120 }
  if (rawNodes.value.length > 50) {
    base.ranksep = 160
    base.nodesep = 24
  }
  return base
}

function render() {
  // TR-1.2 evidence: dev-only render counter at the very top of render()
  console.count('[LineageGraph] render')
  const graph = graphRef.value
  if (!graph) return
  graph.setData(buildData())
  graph.render()
  requestAnimationFrame(() => {
    if (minimapVisible.value) drawMinimap()
  })
  if (props.filterDimEnabled && props.filterKeepIds.length) {
    setTimeout(() => applyFilterDimStates(), 0)
  }
}

function zoomBy(factor) {
  const graph = graphRef.value
  if (!graph) return
  try { graph.zoomBy(factor) } catch (e) { /* ignore */ }
}

function fitView() {
  const graph = graphRef.value
  if (!graph) return
  try { graph.fitView() } catch (e) { /* ignore */ }
}

function resetView() {
  const graph = graphRef.value
  if (!graph) return
  try { graph.zoomTo(1) } catch (e) { /* ignore */ }
}

function ctxDetail() {
  contextMenu.visible = false
  if (contextMenu.node && !contextMenu.isAgg) emit('node-click', contextMenu.node)
}
function ctxRefocus() {
  contextMenu.visible = false
  if (contextMenu.node && !contextMenu.isAgg) emit('node-dblclick', contextMenu.node)
}
function ctxExpand() {
  contextMenu.visible = false
  if (contextMenu.node?.layer != null) expandLayer(contextMenu.node.layer)
}

function expandLayer(layer) {
  expandedLayers.value.add(Number(layer))
  render()
}

function doFullscreen() {
  containerRef.value?.requestFullscreen?.().catch(() => { /* noop */ })
}

// --- P8: applyHighlightStates / clearHighlightStates helpers ---
function clearHighlightStates() {
  const g = graphRef.value
  if (!g) return
  try {
    const edges = g.getEdgeData()
    for (const e of edges) {
      try { g.setElementState(e.id, 'col-highlight', false) } catch (_) { /* ignore */ }
      try { g.setElementState(e.id, 'col-dim', false) } catch (_) { /* ignore */ }
    }
  } catch (_) { /* ignore */ }
}

function applyHighlightStates() {
  const g = graphRef.value
  if (!g) return
  try {
    clearHighlightStates()
    const ids = props.highlightedFieldEdgeIds || []
    if (!ids.length) return
    const idSet = new Set(ids)
    const edges = g.getEdgeData()
    for (const e of edges) {
      if (idSet.has(e.id)) {
        try { g.setElementState(e.id, 'col-highlight', true) } catch (_) { /* ignore */ }
      } else {
        try { g.setElementState(e.id, 'col-dim', true) } catch (_) { /* ignore */ }
      }
    }
  } catch (_) { /* ignore */ }
}

/**
 * Remove any `filter-dim` state previously applied (called on clear / mode switch).
 */
function clearFilterDimStates() {
  const g = graphRef.value
  if (!g) return
  try {
    const nodes = g.getNodeData()
    for (const n of nodes) {
      try { g.setElementState(n.id, 'filter-dim', false) } catch (_) { /* ignore */ }
    }
    const edges = g.getEdgeData()
    for (const e of edges) {
      try { g.setElementState(e.id, 'filter-dim', false) } catch (_) { /* ignore */ }
    }
  } catch (_) { /* ignore */ }
}

/**
 * Incrementally apply the `filter-dim` state to nodes and edges not included in filterKeepIds.
 * Uses G6 incremental setElementState only; never triggers a layout / data rebuild.
 */
function applyFilterDimStates() {
  const g = graphRef.value
  if (!g) return
  try {
    clearFilterDimStates()
    if (!props.filterDimEnabled || !props.filterKeepIds.length) return
    const keepSet = new Set(props.filterKeepIds)
    const nodes = g.getNodeData()
    for (const n of nodes) {
      if (!keepSet.has(n.id)) {
        try { g.setElementState(n.id, 'filter-dim', true) } catch (_) { /* ignore */ }
      }
    }
    const edges = g.getEdgeData()
    for (const e of edges) {
      if (!keepSet.has(e.source) || !keepSet.has(e.target)) {
        try { g.setElementState(e.id, 'filter-dim', true) } catch (_) { /* ignore */ }
      }
    }
  } catch (_) { /* ignore */ }
}

// Watch highlightedFieldEdgeIds: react to column-highlight prop changes
watch(() => props.highlightedFieldEdgeIds, () => {
  applyHighlightStates()
}, { immediate: true, deep: true })

// Watch filterKeepIds / filterDimEnabled: incremental filter-dim state (no re-render)
watch([() => props.filterKeepIds, () => props.filterDimEnabled],
  () => {
    const g = graphRef.value
    if (!g) return
    if (props.filterDimEnabled && props.filterKeepIds.length) applyFilterDimStates()
    else clearFilterDimStates()
  }, { deep: false, immediate: false })

// ---- Subtle pulse animation for FLOWS_TO edges (glow breathing, no dash) ----
function startFlowAnimation() {
  let layoutBusy = false
  const g = graphRef.value
  if (g) {
    g.on('beforelayout', () => { layoutBusy = true })
    g.on('afterlayout', () => { layoutBusy = false })
  }
  const tick = () => {
    const graph = graphRef.value
    if (graph && !layoutBusy) {
      try {
        const data = graph.getEdgeData()
        const pulse = []
        for (const e of data) {
          if (e.data?.relationType === 'FLOWS_TO') {
            const base = 0.65 + 0.35 * (0.5 + 0.5 * Math.sin(flowOffset / 18))
            pulse.push({ id: e.id, style: { opacity: base } })
          }
        }
        if (pulse.length) {
          graph.updateEdgeData(pulse)
          graph.draw({ silence: true })
        }
      } catch (e) { /* skip frame during transient layout race */ }
    }
    flowOffset = (flowOffset + 1) % 120
    animFrame = requestAnimationFrame(tick)
  }
  animFrame = requestAnimationFrame(tick)
}

function stopFlowAnimation() {
  if (animFrame) cancelAnimationFrame(animFrame)
  animFrame = null
}

// ---- Graph lifecycle ----
onMounted(() => {
  const graph = new Graph({
    container: containerRef.value,
    autoFit: 'view',
    data: { nodes: [], edges: [] },
    layout: layoutOption(),
    behaviors: [
      'drag-canvas', 'zoom-canvas', 'drag-element', 'click-select',
      { type: 'hover-activate', degree: 1, activeState: 'active', inactiveState: 'inactive' }
    ],
    node: {
      state: {
        selected: { stroke: '#fbbf24', lineWidth: 4, shadowColor: '#fbbf24', shadowBlur: 14 },
        active: { stroke: '#fde68a', lineWidth: 3 },
        inactive: { opacity: 0.65 },
        'filter-dim': { opacity: 0.15, shadowBlur: 0 }
      }
    },
    edge: {
      state: {
        active: { stroke: '#fde68a', lineWidth: 3.5 },
        inactive: { opacity: 0.4 },
        'col-highlight': { stroke: '#8B5CF6', lineWidth: 3.5, shadowColor: '#8B5CF6', shadowBlur: 10, opacity: 1 },
        'col-dim':       { opacity: 0.2 },
        'filter-dim': { opacity: 0.15 }
      }
    }
  })
  graphRef.value = graph
  try { window.__lineageGraph = graph } catch (e) { /* ignore */ }

  // ---- Custom minimap interactions (P4d/P4e) ----
  let minimapPanTimer = null

  // P4d: pointerdown with drag-or-jump routing
  minimapCanvasRef.value?.addEventListener('pointerdown', (e) => {
    const graph2 = graphRef.value
    const canvas = minimapCanvasRef.value
    if (!graph2 || !canvas) return
    const rect = canvas.getBoundingClientRect()
    const px = e.clientX - rect.left
    const py = e.clientY - rect.top
    const nodes = graph2.getNodeData()
    if (!nodes.length) return
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
    for (const n of nodes) {
      const p = graph2.getElementPosition(n.id)
      if (!p) continue
      if (p[0] < minX) minX = p[0]
      if (p[1] < minY) minY = p[1]
      if (p[0] > maxX) maxX = p[0]
      if (p[1] > maxY) maxY = p[1]
    }
    if (maxX <= minX) maxX = minX + 1
    if (maxY <= minY) maxY = minY + 1
    const pad = 8
    const W = 180, H = 120
    const scale = Math.min((W - pad * 2) / (maxX - minX), (H - pad * 2) / (maxY - minY))
    const ox = (W - (maxX - minX) * scale) / 2 - minX * scale
    const oy = (H - (maxY - minY) * scale) / 2 - minY * scale

    // Compute viewport rect (matches drawMinimap logic)
    let cx = 0, cy = 0, w = 0, h = 0
    try {
      const zoom = graph2.getZoom()
      const vw = containerRef.value?.clientWidth || 0
      const vh = containerRef.value?.clientHeight || 0
      const vc = graph2.getCanvasByViewport([vw / 2, vh / 2])
      w = vw / zoom * scale
      h = vh / zoom * scale
      cx = vc[0] * scale + ox - w / 2
      cy = vc[1] * scale + oy - h / 2
    } catch (_) { /* ignore */ }

    // If pointer falls within viewport rect -> begin drag; otherwise -> jump to center
    if (px >= cx && px <= cx + w && py >= cy && py <= cy + h) {
      minimapDragging = true
      minimapDragOffsetX = px - cx
      minimapDragOffsetY = py - cy
    } else {
      const wx = (px - ox) / scale
      const wy = (py - oy) / scale
      try {
        graph2.translateTo([wx, wy])
        requestAnimationFrame(() => { if (minimapVisible.value) drawMinimap() })
      } catch (err) { /* ignore */ }
    }
  })

  // P4d: pointermove — drag the minimap viewport rectangle
  minimapPointerMoveHandler = (e) => {
    if (!minimapDragging) return
    const graph2 = graphRef.value
    const canvas = minimapCanvasRef.value
    if (!graph2 || !canvas) return
    const rect = canvas.getBoundingClientRect()
    const px = e.clientX - rect.left
    const py = e.clientY - rect.top
    const nodes = graph2.getNodeData()
    if (!nodes.length) return
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
    for (const n of nodes) {
      const p = graph2.getElementPosition(n.id)
      if (!p) continue
      if (p[0] < minX) minX = p[0]
      if (p[1] < minY) minY = p[1]
      if (p[0] > maxX) maxX = p[0]
      if (p[1] > maxY) maxY = p[1]
    }
    if (maxX <= minX) maxX = minX + 1
    if (maxY <= minY) maxY = minY + 1
    const pad = 8
    const W = 180, H = 120
    const scale = Math.min((W - pad * 2) / (maxX - minX), (H - pad * 2) / (maxY - minY))
    const ox = (W - (maxX - minX) * scale) / 2 - minX * scale
    const oy = (H - (maxY - minY) * scale) / 2 - minY * scale
    // Compute current viewport dims
    let w = 0, h = 0
    try {
      const zoom = graph2.getZoom()
      const vw = containerRef.value?.clientWidth || 0
      const vh = containerRef.value?.clientHeight || 0
      w = vw / zoom * scale
      h = vh / zoom * scale
    } catch (_) { /* ignore */ }
    const newCx = px - minimapDragOffsetX
    const newCy = py - minimapDragOffsetY
    const wx = (newCx + w / 2 - ox) / scale
    const wy = (newCy + h / 2 - oy) / scale
    try {
      graph2.translateTo([wx, wy])
      // Throttle via rAF-friendly approach (best-effort on next tick)
      requestAnimationFrame(() => { if (minimapVisible.value) drawMinimap() })
    } catch (_) { /* ignore */ }
  }
  minimapCanvasRef.value?.addEventListener('pointermove', minimapPointerMoveHandler)

  // P4d: window-level pointerup to end a minimap drag
  windowPointerUpHandler = () => {
    minimapDragging = false
    minimapDragOffsetX = 0
    minimapDragOffsetY = 0
  }
  window.addEventListener('pointerup', windowPointerUpHandler)

  // P4e: Wheel zoom on minimap
  minimapWheelHandler = (e) => {
    e.preventDefault()
    const g = graphRef.value
    if (!g) return
    g.zoomBy(e.deltaY > 0 ? 0.87 : 1.15)
  }
  minimapCanvasRef.value?.addEventListener('wheel', minimapWheelHandler, { passive: false })

  // redraw minimap while panning/zooming
  graph.on('canvas:pointermove', (e) => {
    if (!minimapVisible.value) return
    if (e.targetType === 'canvas') {
      clearTimeout(minimapPanTimer)
      minimapPanTimer = setTimeout(() => drawMinimap(), 200)
    }
  })
  graph.on('wheel', () => {
    if (!minimapVisible.value) return
    clearTimeout(minimapPanTimer)
    minimapPanTimer = setTimeout(() => drawMinimap(), 200)
  })

  // ---- P5b: node drag end → emit to parent + dispatch visual CustomEvent ----
  graph.on('node:dragend', (ev) => {
    const id = ev.target?.id
    if (!id) return
    try {
      const [x, y] = graph.getElementPosition(id) || [0, 0]
      emit('node-dragend', { nodeId: id, x, y })
      // Dispatch custom event so parent can show a toast without importing ElMessage here
      if (typeof window !== 'undefined') {
        const ev2 = new CustomEvent('lineage:drag-override-saved', { bubbles: true, detail: { nodeId: id, x, y } })
        containerRef.value?.dispatchEvent(ev2)
      }
    } catch (_) { /* ignore */ }
  })

  function onNodeClick(event) {
    const id = event.target.id
    if (!id) return
    const nodeData = graph.getNodeData(id)
    const data = nodeData?.data || {}
    if (data.nodeType === 'AGG') {
      expandLayer(data.layer)
      return
    }
    emit('node-click', { ...data, id })
  }

  graph.on('node:click', (event) => {
    const data = event.target.id ? (graph.getNodeData(event.target.id)?.data || {}) : {}
    if (data.nodeType === 'AGG') {
      onNodeClick(event)
      return
    }
    clearTimeout(clickTimer)
    clickTimer = setTimeout(() => onNodeClick(event), 260)
  })

  graph.on('node:dblclick', (event) => {
    clearTimeout(clickTimer)
    const id = event.target.id
    if (!id) return
    const data = graph.getNodeData(id)?.data || {}
    if (data.nodeType === 'AGG') {
      expandLayer(data.layer)
      return
    }
    emit('node-dblclick', { ...data, id })
  })

  graph.on('node:contextmenu', (event) => {
    const id = event.target.id
    if (!id) return
    const nodeData = graph.getNodeData(id)
    if (!nodeData) return
    const data = nodeData.data || {}
    contextMenu.node = { ...data, id }
    contextMenu.isAgg = data.nodeType === 'AGG'
    const rect = containerRef.value?.getBoundingClientRect()
    const x = event.client.x - rect.left + 8
    const y = event.client.y - rect.top + 8
    contextMenu.x = Math.min(x, (rect?.width || 400) - 180)
    contextMenu.y = Math.min(y, (rect?.height || 300) - 100)
    contextMenu.visible = true
  })

  function ctxDetail() {
    contextMenu.visible = false
    if (contextMenu.node && !contextMenu.isAgg) emit('node-click', contextMenu.node)
  }
  function ctxRefocus() {
    contextMenu.visible = false
    if (contextMenu.node && !contextMenu.isAgg) emit('node-dblclick', contextMenu.node)
  }
  function ctxExpand() {
    contextMenu.visible = false
    if (contextMenu.node?.layer != null) expandLayer(contextMenu.node.layer)
  }

  // ---- Edge interactions (hover highlight + click detail) ----
  function edgeData(id) {
    return rawEdges.value.find(e => e.id === id) || null
  }
  function nodeName(id) {
    const n = rawNodes.value.find(x => x.id === id)
    return n ? n.name : id
  }

  graph.on('edge:pointerenter', (event) => {
    const id = event.target.id
    if (!id) return
    try {
      graph.setElementState(id, 'active', true)
    } catch (e) { /* ignore */ }
  })

  graph.on('edge:pointerleave', (event) => {
    const id = event.target.id
    if (!id) return
    try {
      graph.setElementState(id, 'active', false)
    } catch (e) { /* ignore */ }
  })

  // P6a: Cache edge data into reactive selectedEdgeData for emit payloads
  graph.on('edge:click', (event) => {
    const id = event.target.id
    if (!id) return
    const e = edgeData(id)
    if (!e) return
    // Store structured edge data for emit payloads (edge-edit / edge-delete)
    selectedEdgeData.value = {
      id: e.id || id,
      source: e.source,
      target: e.target,
      relationType: e.relationType || '',
      taskName: e.taskName || '',
      sql: e.sql || '',
      fieldLinks: e.fieldLinks || []
    }
    const rect = containerRef.value?.getBoundingClientRect()
    edgeInfo.sourceName = nodeName(e.source)
    edgeInfo.targetName = nodeName(e.target)
    edgeInfo.taskName = e.taskName || ''
    edgeInfo.sql = e.sql || ''
    edgeInfo.fieldLinks = e.fieldLinks || []
    edgeInfo.relationLabel = edgeRelationLabel(e.relationType)
    edgeInfo.tagType = edgeTagType(e.relationType)
    edgeInfo.x = Math.min((event.client?.x ?? 0) - rect.left + 12, (rect?.width || 400) - 340)
    edgeInfo.y = Math.min((event.client?.y ?? 0) - rect.top + 12, (rect?.height || 300) - 260)
    edgeInfo.visible = true
  })

  containerRef.value?.addEventListener('pointerdown', (e) => {
    if (e.button === 0 && !(e.target.closest && e.target.closest('.edge-info'))) edgeInfo.visible = false
  })

  containerRef.value?.addEventListener('pointerdown', (e) => {
    if (e.button === 0 && !(e.target.closest && e.target.closest('.ctx-menu'))) contextMenu.visible = false
  })

  graph.on('node:pointerenter', (event) => {
    const id = event.target.id
    if (!id) return
    const nodeData = graph.getNodeData(id)
    if (!nodeData) return
    const d = nodeData.data || {}
    if (d.nodeType === 'AGG') return
    const rels = new Set()
    let sql = ''
    for (const e of rawEdges.value) {
      if (e.source === id || e.target === id) {
        if (e.taskName) rels.add(e.taskName)
        if (!sql && e.sql) sql = e.sql
      }
    }
    tooltip.name = d.name || id
    tooltip.typeTag = typeTag(d)
    tooltip.datasourceName = d.datasourceName || ''
    tooltip.relations = [...rels].slice(0, 3)
    tooltip.sql = sql.length > 90 ? sql.slice(0, 90) + '…' : sql
    tooltip.layer = d.layer ?? 0
    tooltip.deg = degreeMap()[id] || 0
    tooltip.rowCount = d.rowCount != null ? d.rowCount : null
    tooltip.columnCount = d.columnCount != null ? d.columnCount : null
    tooltip.category = d.category || ''
    tooltip.visible = true
    placeTooltip(event)
  })

  graph.on('node:pointermove', placeTooltip)

  graph.on('node:pointerleave', () => {
    tooltip.visible = false
  })

  containerRef.value?.addEventListener('pointerleave', () => {
    tooltip.visible = false
  })

  function placeTooltip(event) {
    if (!tooltip.visible) return
    const rect = containerRef.value?.getBoundingClientRect()
    if (!rect) return
    const x = event.client.x - rect.left + 14
    const y = event.client.y - rect.top + 14
    tooltip.x = Math.min(x, rect.width - 300)
    tooltip.y = Math.min(y, rect.height - 180)
  }

  // P0b + P3: data watch with short-circuit guard
  watch(() => props.data, (d, prev) => {
    if (d === prev) return
    if (!d) return
    rawNodes.value = d.nodes || []
    rawEdges.value = (d.links || []).map((l, i) => ({ id: l.id || 'e' + i, source: l.source, target: l.target, relationType: l.relationType, taskName: l.taskName, sql: l.sql, transform: l.transform, fieldLinks: l.fieldLinks || [] }))
    expandedLayers.value = new Set()
    render()
  }, { deep: false })

  // P0b + P3: layout watch with short-circuit, fitView, and highlight state reapply
  watch(() => props.layout, (l, prev) => {
    if (l === prev) return
    graph.setLayout(layoutOption())
    render()
    fitView()
    nextTick(() => {
      if (props.highlightedFieldEdgeIds && props.highlightedFieldEdgeIds.length) {
        applyHighlightStates()
      } else {
        clearHighlightStates()
      }
      if (props.filterDimEnabled) setTimeout(() => applyFilterDimStates(), 0)
    })
  })

  if (props.data) {
    rawNodes.value = props.data.nodes || []
    rawEdges.value = (props.data.links || []).map((l, i) => ({ id: l.id || 'e' + i, source: l.source, target: l.target, relationType: l.relationType, taskName: l.taskName, sql: l.sql, transform: l.transform, fieldLinks: l.fieldLinks || [] }))
    render()
  }

  startFlowAnimation()
  startMinimapTicker()
})

onBeforeUnmount(() => {
  stopFlowAnimation()
  stopMinimapTicker()
  // P4d / P4e: Clean up newly added global and per-canvas listeners
  if (windowPointerUpHandler) {
    window.removeEventListener('pointerup', windowPointerUpHandler)
    windowPointerUpHandler = null
  }
  if (minimapPointerMoveHandler && minimapCanvasRef.value) {
    minimapCanvasRef.value.removeEventListener('pointermove', minimapPointerMoveHandler)
    minimapPointerMoveHandler = null
  }
  if (minimapWheelHandler && minimapCanvasRef.value) {
    minimapCanvasRef.value.removeEventListener('wheel', minimapWheelHandler)
    minimapWheelHandler = null
  }
  const graph = graphRef.value
  graphRef.value = null
  if (graph) {
    try { graph.stopLayout() } catch (e) { /* ignore */ }
    graph.destroy()
  }
})

defineExpose({
  getRenderedNodeCount() {
    const graph = graphRef.value
    if (!graph) return 0
    try { return graph.getNodeData().length } catch (e) { return 0 }
  },
  getRenderedEdgeCount() {
    const graph = graphRef.value
    if (!graph) return 0
    try { return graph.getEdgeData().length } catch (e) { return 0 }
  },
  async exportPng(lock) {
    const graph = graphRef.value
    if (!graph) return null
    const useLock = lock != null ? !!lock : !!props.screenshotLock
    try {
      const layers = Array.from(containerRef.value?.querySelectorAll('canvas') || [])
        .filter(c => !(c.parentElement && c.parentElement.className && String(c.parentElement.className).includes('minimap')))
      if (!layers.length) return null
      const w = layers[0].width
      const h = layers[0].height
      const canvas = document.createElement('canvas')
      canvas.width = w
      canvas.height = h
      const ctx = canvas.getContext('2d')
      ctx.fillStyle = '#f5f7fa'
      ctx.fillRect(0, 0, w, h)
      for (const layer of layers) {
        ctx.drawImage(layer, 0, 0, w, h)
      }
      // P9c: Composite mode — paint minimap + legend onto output canvas when lock is on
      if (useLock) {
        const mmCanvas = minimapCanvasRef.value
        // Draw minimap at bottom-right (16px padding, proportional scale 2x)
        if (mmCanvas) {
          try {
            const mmW = mmCanvas.width
            const mmH = mmCanvas.height
            const scale2x = 2.0
            const drawW = mmW * scale2x
            const drawH = mmH * scale2x
            const pad = 16
            const dx = w - drawW - pad
            const dy = h - drawH - pad
            ctx.save()
            // minimap outline
            ctx.fillStyle = '#FFFFFF'
            ctx.fillRect(dx - 2, dy - 2, drawW + 4, drawH + 4)
            ctx.strokeStyle = '#E0E0E0'
            ctx.lineWidth = 1
            ctx.strokeRect(dx - 2.5, dy - 2.5, drawW + 5, drawH + 5)
            ctx.drawImage(mmCanvas, 0, 0, mmW, mmH, dx, dy, drawW, drawH)
            ctx.restore()
          } catch (_) { /* ignore composit draw failure */ }
        }
        // Draw legend panel (top-right) manually using fillRect + fillText
        try {
          const pad = 16
          const sw = 12
          const lh = 18
          const lx = w - 220 - pad
          const ly = pad
          // Background card
          ctx.save()
          ctx.fillStyle = '#FFFFFF'
          ctx.strokeStyle = '#E0E0E0'
          ctx.lineWidth = 1
          const totalRows = 1 + legendNodes.length + 1 + legendEdges.length + 1
          const cardW = 220
          const cardH = 16 + 16 + (legendNodes.length + 1) * lh + (legendEdges.length + 1) * lh + 8
          ctx.fillRect(lx, ly, cardW, cardH)
          ctx.strokeRect(lx + 0.5, ly + 0.5, cardW - 1, cardH - 1)
          ctx.font = 'bold 12px Menlo, Consolas, monospace'
          ctx.fillStyle = '#263238'
          let cy = ly + 18
          ctx.fillText('Legend', lx + 12, cy)
          cy += lh
          ctx.font = '10px Menlo, Consolas, monospace'
          ctx.fillStyle = '#78909C'
          ctx.fillText('Node Types', lx + 12, cy)
          cy += lh
          ctx.font = '11px Menlo, Consolas, monospace'
          for (const n of legendNodes) {
            ctx.fillStyle = n.color
            ctx.fillRect(lx + 12, cy - 10, sw, sw)
            ctx.fillStyle = '#546E7A'
            ctx.fillText(n.label, lx + 12 + sw + 8, cy - 1)
            cy += lh
          }
          cy += 4
          ctx.font = '10px Menlo, Consolas, monospace'
          ctx.fillStyle = '#78909C'
          ctx.fillText('Edge Types', lx + 12, cy)
          cy += lh
          ctx.font = '11px Menlo, Consolas, monospace'
          for (const e of legendEdges) {
            ctx.strokeStyle = e.color
            ctx.lineWidth = 2
            ctx.setLineDash(e.dash ? [4, 3] : [])
            ctx.beginPath()
            ctx.moveTo(lx + 12, cy - 5)
            ctx.lineTo(lx + 12 + 22, cy - 5)
            ctx.stroke()
            ctx.setLineDash([])
            ctx.fillStyle = '#546E7A'
            ctx.fillText(e.label, lx + 12 + 22 + 8, cy - 1)
            cy += lh
          }
          ctx.restore()
        } catch (_) { /* ignore legend draw failure */ }
      }
      return canvas.toDataURL('image/png')
    } catch (e) {
      try {
        return await graph.toDataURL()
      } catch (e2) {
        return null
      }
    }
  }
})
</script>

<style scoped>
.lineage-g6-container {
  width: 100%;
  height: 100%;
  background:
    linear-gradient(rgba(120, 144, 156, 0.12) 1px, transparent 1px),
    linear-gradient(90deg, rgba(120, 144, 156, 0.12) 1px, transparent 1px),
    radial-gradient(ellipse at 30% 20%, #ffffff 0%, #f5f7fa 60%, #eef2f6 100%);
  background-size: 24px 24px, 24px 24px, 100% 100%;
  position: relative;
  overflow: hidden;
}

.canvas-actions {
  position: absolute;
  bottom: 16px;
  left: 16px;
  z-index: 12;
  display: flex;
  align-items: center;
  gap: 4px;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 4px;
  backdrop-filter: blur(4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}
.ca-btn {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #546e7a;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: Menlo, Consolas, monospace;
}
.ca-btn:hover {
  background: #eef2f6;
  color: #1f2937;
}
.ca-btn.active {
  background: #e3f2fd;
  color: #4a90e2;
}
.ca-sep {
  width: 1px;
  height: 18px;
  background: #e0e0e0;
  margin: 0 2px;
}

.ctx-menu {
  position: absolute;
  z-index: 30;
  min-width: 140px;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.14);
  padding: 4px;
  font-size: 12px;
  color: #334155;
}
.ctx-item {
  padding: 7px 10px;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
}
.ctx-item:hover {
  background: #eef2f6;
  color: #1f2937;
}
/* P10: disabled context-menu item styling */
.ctx-item.disabled {
  color: #B0BEC5;
  cursor: not-allowed;
  background: transparent;
}
.ctx-item.disabled:hover {
  background: transparent;
  color: #B0BEC5;
}

.lineage-tooltip {
  position: absolute;
  z-index: 20;
  pointer-events: none;
  width: 280px;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-left: 3px solid #4a90e2;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
  padding: 10px 12px;
  font-family: Menlo, Consolas, monospace;
  font-size: 11px;
  line-height: 1.55;
  color: #37474f;
}

.lt-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.lt-type {
  flex-shrink: 0;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #ffffff;
  background: #4a90e2;
  border-radius: 3px;
  padding: 1px 5px;
}

.lt-name {
  font-weight: 700;
  color: #263238;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lt-row {
  display: flex;
  gap: 8px;
  margin-top: 3px;
}

.lt-k {
  flex-shrink: 0;
  color: #78909c;
}

.lt-v {
  color: #37474f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lt-sql {
  white-space: normal;
  word-break: break-all;
  max-height: 56px;
  overflow: hidden;
}

.lt-divider {
  height: 1px;
  background: #eef2f6;
  margin: 6px 0;
}

/* ---- 小地图 ---- */
.lineage-minimap {
  position: absolute;
  bottom: 16px;
  right: 16px;
  z-index: 12;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  cursor: crosshair;
  background: #f5f7fa;
}
.lineage-minimap canvas {
  display: block;
}

/* ---- 图例 ---- */
.legend-panel {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 12;
  min-width: 190px;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 10px 12px;
  backdrop-filter: blur(4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  font-family: Menlo, Consolas, monospace;
  font-size: 11px;
  color: #37474f;
}
.legend-title {
  font-weight: 700;
  color: #263238;
  margin-bottom: 8px;
  font-size: 12px;
}
.legend-section {
  margin-bottom: 8px;
}
.legend-section:last-child {
  margin-bottom: 0;
}
.legend-sub {
  color: #78909c;
  font-size: 10px;
  margin-bottom: 4px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
}
.legend-swatch {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  flex-shrink: 0;
}
.legend-line {
  width: 22px;
  border-top-width: 2px;
  border-top-style: solid;
  flex-shrink: 0;
}
.legend-label {
  color: #546e7a;
}

/* ---- 边详情浮层 ---- */
.edge-info {
  position: absolute;
  z-index: 25;
  width: 320px;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.14);
  font-family: Menlo, Consolas, monospace;
  font-size: 11px;
  color: #37474f;
  overflow: hidden;
}
.edge-info-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #eef2f6;
}
.edge-info-close {
  border: none;
  background: transparent;
  color: #78909c;
  font-size: 16px;
  cursor: pointer;
  line-height: 1;
}
.edge-info-close:hover {
  color: #1f2937;
}
.edge-info-body {
  padding: 10px 12px;
}
.edge-info-row {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}
.ei-k {
  flex-shrink: 0;
  color: #78909c;
  min-width: 32px;
}
.ei-v {
  color: #37474f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ei-sql {
  white-space: normal;
  word-break: break-all;
  max-height: 60px;
  overflow: hidden;
}
.edge-info-fields {
  margin-top: 8px;
  border-top: 1px solid #1e293b;
  padding-top: 6px;
}
.edge-info-sub {
  color: #cbd5e1;
  margin-bottom: 4px;
}
.edge-info-field {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
  font-size: 10px;
}
.eif-s {
  color: #10b981;
}
.eif-t {
  color: #f59e0b;
}

/* P10: Edge-info action buttons */
.edge-info-actions {
  display: flex;
  gap: 6px;
  padding-top: 8px;
  margin-top: 8px;
  border-top: 1px solid #EEF2F6;
}
.ei-btn {
  padding: 5px 10px;
  border-radius: 4px;
  background: transparent;
  border: 1px solid #E0E0E0;
  font-family: Menlo, Consolas, monospace;
  font-size: 11px;
  color: #37474f;
  cursor: pointer;
  line-height: 1.2;
}
.ei-btn:hover:not(:disabled) {
  background: #eef2f6;
  border-color: #b0bec5;
}
.ei-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.ei-btn-danger {
  color: #EF4444;
}
.ei-btn-danger:hover:not(:disabled) {
  background: #fef2f2;
  border-color: #fca5a5;
}
</style>
