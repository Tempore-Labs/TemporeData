<template>
  <div class="lineage-canvas">
    <div ref="container" class="g6-container" />
    <div class="zoom-controls">
      <button title="放大" @click="zoom(1.2)">＋</button>
      <button title="缩小" @click="zoom(0.8)">−</button>
      <button title="适配视图" @click="fitView">⊡</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Graph } from '@antv/g6'
import type { LineageEdge, LineageNode } from '@/types/lineage'

const TYPE_COLORS: Record<string, { bg: string; border: string; text: string; badge: string }> = {
  table: { bg: '#f0f9ff', border: '#2563eb', text: '#1e40af', badge: 'T' },
  view: { bg: '#f0fdf4', border: '#10b981', text: '#065f46', badge: 'V' },
  kafka: { bg: '#fef3c7', border: '#f59e0b', text: '#92400e', badge: 'K' },
  api: { bg: '#faf5ff', border: '#8b5cf6', text: '#5b21b6', badge: 'A' },
}

const props = withDefaults(
  defineProps<{
    nodes: LineageNode[]
    links: LineageEdge[]
    selectedId?: string | null
    searchMatch?: string[] | null
  }>(),
  { selectedId: null, searchMatch: null },
)

const emit = defineEmits<{
  (e: 'select', id: string | null): void
}>()

const container = ref<HTMLDivElement>()
let graph: Graph | null = null

const edgeId = (e: LineageEdge) => `${e.source}-${e.target}`

function buildData(): any {
  const nodes = props.nodes.map((n) => {
    const t = (n.type || 'table') as string
    const c = TYPE_COLORS[t] || TYPE_COLORS.table
    const label = n.label || n.name || n.id
    const style: any = {
      fill: '#ffffff',
      stroke: c.border,
      lineWidth: 1.5,
      radius: 8,
      labelText: label,
      labelFill: '#0f172a',
      labelFontSize: 10.5,
      labelFontFamily: 'JetBrains Mono, monospace',
      labelPlacement: 'left',
      labelOffsetX: 12,
      labelMaxWidth: 140,
      iconText: c.badge,
      iconFill: c.text,
      iconBackgroundFill: c.bg,
      iconBackgroundRadius: 4,
      iconWidth: 16,
      iconHeight: 16,
      iconFontSize: 9,
      iconOffsetX: -64,
      badge: false,
    }
    if (n.schema && n.owner) {
      style.secondaryLabelText = `${n.schema} · ${n.owner}`
      style.secondaryLabelFill = '#94a3b8'
      style.secondaryLabelFontSize = 9
      style.secondaryLabelOffsetY = 14
      style.secondaryLabelPlacement = 'left'
      style.secondaryLabelOffsetX = 12
    }
    return { id: n.id, style }
  })
  const edges = props.links.map((e) => ({
    id: edgeId(e),
    source: e.source,
    target: e.target,
    style: {
      stroke: '#cbd5e1',
      lineWidth: 1.5,
      strokeDasharray: '4 3',
      endArrow: true,
      endArrowFill: '#94a3b8',
    },
  }))
  return { nodes, edges }
}

const NODE_STATE: any = {
  selected: { lineWidth: 2.5, stroke: '#2563eb', labelFill: '#2563eb' },
  'filter-dim': { opacity: 0.15 },
  related: { stroke: '#7db6fb', lineWidth: 2 },
}
const EDGE_STATE: any = {
  related: { stroke: '#2563eb', lineWidth: 2, strokeDasharray: undefined, endArrowFill: '#2563eb' },
  'filter-dim': { opacity: 0.2 },
}

async function init() {
  if (!container.value || graph) return
  graph = new Graph({
    container: container.value,
    autoResize: true,
    data: buildData(),
    node: { type: 'rect', state: NODE_STATE },
    edge: { type: 'cubic-horizontal', state: EDGE_STATE },
    layout: { type: 'dagre', rankdir: 'LR', nodesep: 24, ranksep: 110 },
    behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
  })

  graph.on('node:click', (evt: any) => {
    const id = evt.target.id as string
    emit('select', props.selectedId === id ? null : id)
  })
  graph.on('canvas:click', () => emit('select', null))

  await graph.render()
  fitView()
  applyHighlights()
}

function applyHighlights() {
  if (!graph) return
  const sel = props.selectedId
  try {
    const related = new Set<string>()
    if (sel) {
      props.links.forEach((e) => {
        if (e.source === sel || e.target === sel) {
          related.add(e.source)
          related.add(e.target)
          graph?.setElementState(edgeId(e), 'related', true)
        } else {
          graph?.setElementState(edgeId(e), 'related', false)
        }
      })
      props.nodes.forEach((n) => {
        const isSel = n.id === sel
        const isRel = related.has(n.id)
        graph?.setElementState(n.id, 'selected', isSel)
        graph?.setElementState(n.id, 'related', isRel && !isSel)
      })
    } else {
      props.nodes.forEach((n) => {
        graph?.setElementState(n.id, 'selected', false)
        graph?.setElementState(n.id, 'related', false)
      })
      props.links.forEach((e) => graph?.setElementState(edgeId(e), 'related', false))
    }
    if (props.searchMatch && props.searchMatch.length) {
      props.nodes.forEach((n) => {
        graph?.setElementState(n.id, 'filter-dim', !props.searchMatch?.includes(n.id))
      })
      props.links.forEach((e) => {
        const ok = props.searchMatch!.includes(e.source) || props.searchMatch!.includes(e.target)
        graph?.setElementState(edgeId(e), 'filter-dim', !ok)
      })
    } else {
      props.nodes.forEach((n) => graph?.setElementState(n.id, 'filter-dim', false))
      props.links.forEach((e) => graph?.setElementState(edgeId(e), 'filter-dim', false))
    }
  } catch {
    // State API differences between G6 minor versions degrade gracefully.
  }
}

function zoom(factor: number) {
  try {
    ;(graph as any)?.zoomBy(factor)
  } catch {
    /* noop */
  }
}
function fitView() {
  try {
    ;(graph as any)?.fitView({ padding: 24 })
  } catch {
    /* noop */
  }
}

watch(
  () => [props.nodes, props.links],
  () => {
    if (graph && container.value) {
      graph.setData(buildData())
      graph.render().then(() => {
        fitView()
        applyHighlights()
      })
    }
  },
  { deep: true },
)

watch(() => props.selectedId, applyHighlights)
watch(() => props.searchMatch, applyHighlights, { deep: true })

onMounted(init)
onBeforeUnmount(() => {
  graph?.destroy()
  graph = null
})
</script>

<style scoped>
.lineage-canvas {
  position: relative;
  flex: 1;
  min-width: 0;
  height: 100%;
  background-color: #f8fafc;
  background-image: radial-gradient(circle, #e2e8f0 1px, transparent 1px);
  background-size: 24px 24px;
}
.g6-container {
  width: 100%;
  height: 100%;
}
.zoom-controls {
  position: absolute;
  bottom: 16px;
  right: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.zoom-controls button {
  width: 28px;
  height: 28px;
  border: 1px solid var(--td-border);
  border-radius: 8px;
  background: #fff;
  color: var(--td-text-2);
  font-size: 14px;
  cursor: pointer;
  box-shadow: var(--td-card-shadow);
}
.zoom-controls button:hover {
  background: var(--td-border-light);
}
</style>
