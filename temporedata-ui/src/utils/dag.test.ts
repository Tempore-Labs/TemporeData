// @vitest-environment node
import { describe, expect, it } from 'vitest'
import {
  NODE_STATUS_COLOR,
  NODE_TEMPLATES,
  NODE_TYPE_COLOR,
  autoLayout,
  danglingEdges,
  decode,
  encode,
  isDag,
  statusOf,
  uid,
  type DagEdgeVM,
  type DagNodeVM,
  type NodeStatus,
} from './dag'

/* ---------------------------------------------------------------------------
 * encode / decode — DTO ⇄ view-model boundary
 * ------------------------------------------------------------------------- */
describe('encode / decode', () => {
  it('encode strips transient UI-only fields before persisting', () => {
    const node: DagNodeVM = {
      id: 'n1', name: '取数', type: 'SQL', sql: 'select 1',
      positionX: 40, positionY: 20,
      _selected: true, _status: 'RUNNING', _errorMsg: 'boom',
    }
    const edge: DagEdgeVM = {
      id: 'e1', sourceNodeId: 'n1', targetNodeId: 'n2', edgeType: 'SUCCESS', _active: true,
    }
    const { nodes, edges } = encode([node], [edge])
    expect(nodes[0]).not.toHaveProperty('_selected')
    expect(nodes[0]).not.toHaveProperty('_status')
    expect(nodes[0]).not.toHaveProperty('_errorMsg')
    expect(nodes[0]).toMatchObject({ id: 'n1', sql: 'select 1', positionX: 40, positionY: 20 })
    expect(edges[0]).not.toHaveProperty('_active')
    expect(edges[0]).toMatchObject({ sourceNodeId: 'n1', targetNodeId: 'n2', edgeType: 'SUCCESS' })
  })

  it('encode drops edges that are missing an endpoint', () => {
    const incomplete = { id: 'e0', sourceNodeId: 'n1', targetNodeId: '' } as DagEdgeVM
    const { edges } = encode([], [incomplete])
    expect(edges).toHaveLength(0)
  })

  it('decode attaches idle UI status to nodes', () => {
    const { nodes } = decode([{ id: 'n1', name: 'a', type: 'SQL' }], [])
    expect(nodes[0]._status).toBe('idle')
    expect(nodes[0].id).toBe('n1')
  })

  it('decode with no args yields empty collections', () => {
    const { nodes, edges } = decode(undefined, undefined)
    expect(nodes).toEqual([])
    expect(edges).toEqual([])
  })
})

/* ---------------------------------------------------------------------------
 * isDag — Kahn cycle detection
 * ------------------------------------------------------------------------- */
describe('isDag', () => {
  it('accepts a connected acyclic graph', () => {
    const nodes = [{ id: 'a' }, { id: 'b' }, { id: 'c' }]
    const edges = [
      { sourceNodeId: 'a', targetNodeId: 'b' },
      { sourceNodeId: 'b', targetNodeId: 'c' },
    ]
    expect(isDag(nodes, edges)).toBe(true)
  })

  it('rejects a cycle', () => {
    const nodes = [{ id: 'a' }, { id: 'b' }, { id: 'c' }]
    const edges = [
      { sourceNodeId: 'a', targetNodeId: 'b' },
      { sourceNodeId: 'b', targetNodeId: 'c' },
      { sourceNodeId: 'c', targetNodeId: 'a' },
    ]
    expect(isDag(nodes, edges)).toBe(false)
  })

  it('rejects a self loop', () => {
    const nodes = [{ id: 'a' }]
    const edges = [{ sourceNodeId: 'a', targetNodeId: 'a' }]
    expect(isDag(nodes, edges)).toBe(false)
  })

  it('treats an empty graph as a valid DAG', () => {
    expect(isDag([], [])).toBe(true)
  })

  it('ignores edges referencing unknown nodes (does not corrupt result)', () => {
    const nodes = [{ id: 'a' }]
    const edges = [{ sourceNodeId: 'x', targetNodeId: 'y' }]
    expect(isDag(nodes, edges)).toBe(true)
  })
})

/* ---------------------------------------------------------------------------
 * danglingEdges
 * ------------------------------------------------------------------------- */
describe('danglingEdges', () => {
  it('returns only edges whose endpoints are missing', () => {
    const nodes = [{ id: 'a' }, { id: 'b' }]
    const edges = [
      { sourceNodeId: 'a', targetNodeId: 'b' },
      { sourceNodeId: 'a', targetNodeId: 'ghost' },
      { sourceNodeId: 'ghost', targetNodeId: 'b' },
    ]
    const bad = danglingEdges(nodes, edges)
    expect(bad).toHaveLength(2)
    expect(bad.map((e) => e.sourceNodeId)).toContain('ghost')
    expect(bad.map((e) => e.targetNodeId)).toContain('ghost')
  })

  it('returns empty when all edges are well-formed', () => {
    expect(danglingEdges([{ id: 'a' }], [{ sourceNodeId: 'a', targetNodeId: 'a' }])).toHaveLength(0)
  })
})

/* ---------------------------------------------------------------------------
 * uid
 * ------------------------------------------------------------------------- */
describe('uid', () => {
  it('produces unique ids with the given prefix', () => {
    const ids = new Set(Array.from({ length: 500 }, () => uid('n')))
    expect(ids.size).toBe(500)
    expect([...ids].every((id) => id.startsWith('n'))).toBe(true)
  })
})

/* ---------------------------------------------------------------------------
 * autoLayout — topological layered layout
 * ------------------------------------------------------------------------- */
describe('autoLayout', () => {
  const n = (id: string): DagNodeVM => ({ id, name: id, type: 'SQL' })

  it('returns one coordinate per node such that downstream sits to the right', () => {
    const nodes = [n('a'), n('b')]
    const edges: DagEdgeVM[] = [{ id: 'e', sourceNodeId: 'a', targetNodeId: 'b' }]
    const coords = autoLayout(nodes, edges)
    expect(coords).toHaveLength(2)
    expect(coords[1].x).toBeGreaterThan(coords[0].x)
  })

  it('gives branch targets a larger x than their single source', () => {
    const nodes = [n('a'), n('b'), n('c')]
    const edges: DagEdgeVM[] = [
      { id: 'e1', sourceNodeId: 'a', targetNodeId: 'b' },
      { id: 'e2', sourceNodeId: 'a', targetNodeId: 'c' },
    ]
    const coords = autoLayout(nodes, edges)
    expect(coords[1].x).toBeGreaterThan(coords[0].x)
    expect(coords[2].x).toBeGreaterThan(coords[0].x)
  })

  it('handles a single isolated node', () => {
    const coords = autoLayout([n('a')], [])
    expect(coords).toHaveLength(1)
    expect(coords[0].x).toBeGreaterThanOrEqual(0)
  })
})

/* ---------------------------------------------------------------------------
 * statusOf — instance → node status map
 * ------------------------------------------------------------------------- */
describe('statusOf', () => {
  it('maps instance nodes by nodeId', () => {
    const map = statusOf({
      nodes: [
        { nodeId: 'n1', status: 'RUNNING' },
        { nodeId: 'n2', status: 'FAILED' },
      ],
    })
    expect(map.get('n1')).toBe('RUNNING')
    expect(map.get('n2')).toBe('FAILED')
    expect(map.get('ghost')).toBeUndefined()
  })

  it('tolerates a missing nodes list', () => {
    expect(statusOf({}).size).toBe(0)
  })
})

/* ---------------------------------------------------------------------------
 * palette / status color completeness
 * ------------------------------------------------------------------------- */
describe('node visual mapping', () => {
  it('provides a color for every node template type', () => {
    NODE_TEMPLATES.forEach((t) => {
      expect(NODE_TYPE_COLOR[t.type]).toBe(t.color)
    })
  })

  it('provides a fill for every runtime status', () => {
    const statuses: NodeStatus[] = ['idle', 'WAITING', 'PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'SKIPPED']
    statuses.forEach((s) => {
      expect(NODE_STATUS_COLOR[s]).toBeTypeOf('string')
    })
  })

  it('defines the expected node templates', () => {
    expect(NODE_TEMPLATES.map((t) => t.type).sort()).toEqual(
      ['FLINK', 'HTTP', 'PYTHON', 'QUALITY', 'SHELL', 'SPARK', 'SQL'],
    )
  })
})