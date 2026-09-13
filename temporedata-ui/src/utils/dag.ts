/**
 * DAG helpers for the workflow editor.
 * Keeps the editable view-model separate from the transport DTO so the
 * frontend can freely own geometry/selection/live status without touching
 * the JSON structure persisted to the backend.
 */
import type { WorkflowEdgeDTO, WorkflowNodeDTO } from '@/api/workflow'

export type NodeStatus =
  | 'idle'
  | 'WAITING'
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCESS'
  | 'FAILED'
  | 'SKIPPED'

/** View-model: transport DTO + transient UI state (never sent to backend). */
export interface DagNodeVM extends WorkflowNodeDTO {
  _selected?: boolean
  _status?: NodeStatus
  _errorMsg?: string
}

export interface DagEdgeVM extends WorkflowEdgeDTO {
  _selected?: boolean
  _active?: boolean
}

/** A node template usable in the left palette. */
export interface NodeTemplate {
  type: WorkflowNodeDTO['type']
  label: string
  color: string
  defaults: Partial<WorkflowNodeDTO>
}

export const NODE_TEMPLATES: NodeTemplate[] = [
  { type: 'SQL', label: 'SQL', color: '#409eff', defaults: { sql: '' } },
  { type: 'SHELL', label: 'Shell', color: '#67c23a', defaults: { params: '' } },
  { type: 'PYTHON', label: 'Python', color: '#e6a23c', defaults: { params: '' } },
  { type: 'FLINK', label: 'Flink', color: '#f56c6c', defaults: { sparkConf: '', params: '' } },
  { type: 'SPARK', label: 'Spark', color: '#f0823b', defaults: { sparkConf: '', params: '' } },
  { type: 'HTTP', label: 'HTTP', color: '#8b5cf6', defaults: { httpMethod: 'GET', httpUrl: '' } },
  { type: 'QUALITY', label: '质量检测', color: '#00b0a1', defaults: { qualityThreshold: 90 } },
]

export const NODE_TYPE_COLOR: Record<string, string> = Object.fromEntries(
  NODE_TEMPLATES.map((t) => [t.type, t.color]),
)

/** Per-node status → color fill used to restyle canvas cells. */
export const NODE_STATUS_COLOR: Record<NodeStatus, string> = {
  idle: '#ffffff',
  WAITING: '#f5f7fa',
  PENDING: '#f5f7fa',
  RUNNING: '#c6e2ff',
  SUCCESS: '#d4edda',
  FAILED: '#f8d7da',
  SKIPPED: '#eceef1',
}

export const NODE_STATUS_LABEL: Record<NodeStatus, string> = {
  idle: '',
  WAITING: '等待',
  PENDING: '排队',
  RUNNING: '运行中',
  SUCCESS: '成功',
  FAILED: '失败',
  SKIPPED: '跳过',
}

// ---------------------------------------------------------------------------
// encode / decode — the only boundary that touches the transport JSON shape
// ---------------------------------------------------------------------------

/** Strip UI-only fields before persisting. */
export function encode(nodes: DagNodeVM[], edges: DagEdgeVM[]): {
  nodes: WorkflowNodeDTO[]
  edges: WorkflowEdgeDTO[]
} {
  const strip = (n: DagNodeVM): WorkflowNodeDTO => {
    const { _selected, _status, _errorMsg, ...dto } = n
    return dto
  }
  const stripEdge = (e: DagEdgeVM): WorkflowEdgeDTO => {
    const { _selected, _active, ...dto } = e
    return dto
  }
  return {
    nodes: nodes.map(strip),
    edges: edges
      .filter((e) => e.sourceNodeId && e.targetNodeId)
      .map(stripEdge),
  }
}

/** Attach idle UI state when loading a persisted workflow. */
export function decode(nodes: WorkflowNodeDTO[] = [], edges: WorkflowEdgeDTO[] = []): {
  nodes: DagNodeVM[]
  edges: DagEdgeVM[]
} {
  return {
    nodes: nodes.map((n) => ({ ...n, _status: 'idle' })),
    edges: edges.map((e) => ({ ...e })),
  }
}

// ---------------------------------------------------------------------------
// validation
// ---------------------------------------------------------------------------

/** Kahn topological sort → true when graph is acyclic (valid DAG). */
export function isDag(
  nodes: { id: string }[],
  edges: { sourceNodeId: string; targetNodeId: string }[],
): boolean {
  const indeg = new Map<string, number>()
  const adj = new Map<string, string[]>()
  nodes.forEach((n) => {
    indeg.set(n.id, 0)
    adj.set(n.id, [])
  })
  edges.forEach((e) => {
    if (!indeg.has(e.sourceNodeId) || !indeg.has(e.targetNodeId)) return
    adj.get(e.sourceNodeId)!.push(e.targetNodeId)
    indeg.set(e.targetNodeId, (indeg.get(e.targetNodeId) ?? 0) + 1)
  })
  const queue = nodes.filter((n) => indeg.get(n.id) === 0).map((n) => n.id)
  let count = 0
  while (queue.length) {
    const id = queue.shift()!
    count++
    for (const next of adj.get(id) ?? []) {
      const d = (indeg.get(next) ?? 0) - 1
      indeg.set(next, d)
      if (d === 0) queue.push(next)
    }
  }
  return count === nodes.length
}

/** Names of nodes that would be unreachable given a broken edge set. */
export function danglingEdges(
  nodes: { id: string }[],
  edges: { sourceNodeId: string; targetNodeId: string }[],
): { sourceNodeId: string; targetNodeId: string }[] {
  const ids = new Set(nodes.map((n) => n.id))
  return edges.filter((e) => !ids.has(e.sourceNodeId) || !ids.has(e.targetNodeId))
}

/** unique id generator for new cells. */
export function uid(prefix = 'n'): string {
  return `${prefix}${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}`
}

// ---------------------------------------------------------------------------
// layout
// ---------------------------------------------------------------------------

/**
 * Layered (topological) grid layout: nodes placed by layer depth left→right,
 * top→bottom. Returns nodes with positionX/positionY filled for DTOs lacking
 * coordinates.
 */
export function autoLayout(nodes: DagNodeVM[], edges: DagEdgeVM[]): { x: number; y: number }[] {
  const spanW = 240
  const spanH = 110
  const layer = new Map<string, number>()
  const adj = new Map<string, string[]>()
  const indeg = new Map<string, number>()
  nodes.forEach((n) => {
    layer.set(n.id, 0)
    adj.set(n.id, [])
    indeg.set(n.id, 0)
  })
  edges.forEach((e) => {
    if (!adj.has(e.sourceNodeId) || !adj.has(e.targetNodeId)) return
    adj.get(e.sourceNodeId)!.push(e.targetNodeId)
    indeg.set(e.targetNodeId, (indeg.get(e.targetNodeId) ?? 0) + 1)
  })
  // longest-path layer assignment (Kahn)
  const indeg2 = new Map(indeg)
  const queue = nodes.filter((n) => indeg2.get(n.id) === 0).map((n) => n.id)
  while (queue.length) {
    const id = queue.shift()!
    for (const next of adj.get(id) ?? []) {
      layer.set(next, Math.max(layer.get(next) ?? 0, (layer.get(id) ?? 0) + 1))
      indeg2.set(next, (indeg2.get(next) ?? 0) - 1)
      if (indeg2.get(next) === 0) queue.push(next)
    }
  }
  const byLayer = new Map<number, string[]>()
  nodes.forEach((n) => {
    const l = layer.get(n.id) ?? 0
    byLayer.set(l, [...(byLayer.get(l) ?? []), n.id])
  })
  const coords = new Map<string, { x: number; y: number }>()
  const layers = [...byLayer.keys()].sort((a, b) => a - b)
  layers.forEach((l) => {
    const list = byLayer.get(l) ?? []
    const off = (nodes.length - list.length) * (spanH / 2)
    list.forEach((id, i) => {
      coords.set(id, { x: 40 + l * spanW, y: 40 + off + i * spanH })
    })
  })
  return nodes.map((n) => coords.get(n.id) ?? { x: 40, y: 40 })
}

// ---------------------------------------------------------------------------
// instance-status mapping for reactive coloring
// ---------------------------------------------------------------------------

export function statusOf(
  instance: { nodes?: { nodeId?: string; status: NodeStatus }[] },
): Map<string, NodeStatus> {
  const map = new Map<string, NodeStatus>()
  instance.nodes?.forEach((n) => {
    if (n.nodeId) map.set(n.nodeId, n.status)
  })
  return map
}