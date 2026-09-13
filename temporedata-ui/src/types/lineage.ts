/** Data lineage types (/api/lineage). */

export type LineageNodeType = 'table' | 'view' | 'kafka' | 'api'

export interface LineageNode {
  id: string
  name?: string
  label?: string
  schema?: string
  type?: LineageNodeType | string
  nodeType?: string
  owner?: string
  domain?: string
  cols?: string[]
}

export interface LineageEdge {
  source: string
  target: string
}

export interface LineageGraph {
  nodes: LineageNode[]
  links: LineageEdge[]
  edges?: LineageEdge[] // backend also serializes an `edges` alias
  rootTableName?: string
  totalUpstream?: number
  totalDownstream?: number
  maxDepth?: number
}

export interface LineageSearchItem {
  nodeId: string
  name: string
  nodeType: string
}

export interface LineageOverview {
  totalNodes: number
  totalEdges: number
  maxDepth: number
  domains?: { name: string; count: number }[]
}
