import api from '../index'

const enc = encodeURIComponent

/**
 * Enterprise lineage module API (graph model: nodes/edges).
 */
export const lineageApi = {
  rebuild: () => api.post('/api/lineage/rebuild').then(r => r.data.data),
  getGraph: (params) => api.get('/api/lineage/graph', { params }).then(r => r.data.data),
  getOverview: () => api.get('/api/lineage/overview').then(r => r.data.data),
  getImpact: (params) => api.get('/api/lineage/impact', { params }).then(r => r.data.data),
  getTrace: (params) => api.get('/api/lineage/lineage', { params }).then(r => r.data.data),
  findPath: (data) => api.post('/api/lineage/path', data).then(r => r.data.data),
  findCycles: () => api.get('/api/lineage/cycles').then(r => r.data.data),
  getHeat: () => api.get('/api/lineage/heat').then(r => r.data.data),
  search: (keyword) => api.get('/api/lineage/search', { params: { keyword } }).then(r => r.data.data),
  exportJson: (params) => api.get('/api/lineage/export', { params }).then(r => r.data.data),
  // Real cross-module SQL parsing (workflow-lineage Druid parser) for IDE live analysis
  parseSql: (sql, datasourceType) => api.post('/api/workflow-lineage/parse', { sql, datasourceType }).then(r => r.data.data),

  // Create or upsert a lineage edge between two entity IDs.
  saveEdge: (fromId, toId, payload) => api.put(`/api/lineage/edge/${enc(fromId)}/${enc(toId)}`, payload || {}).then(r => r.data?.data ?? r.data),
  // Patch partial fields on an existing edge (relationType / task / sql / description …).
  patchEdge: (fromId, toId, patch) => api.patch(`/api/lineage/edge/${enc(fromId)}/${enc(toId)}`, patch || {}).then(r => r.data?.data ?? r.data),
  // Delete a specific edge between two nodes.
  deleteEdge: (fromId, toId) => api.delete(`/api/lineage/edge/${enc(fromId)}/${enc(toId)}`).then(r => r.data?.data ?? r.data),
  // Pull data-quality score map { nodeId: score(0..100) } for every node that has DQ info.
  getDataQuality: () => api.get('/api/lineage/dataquality').then(r => r.data?.data ?? r.data),
  // Hydrate missing attributes (columns / owner / tags / domain) on a batch of node IDs.
  hydrate: (payload) => api.post('/api/lineage/hydrate', payload || {}).then(r => r.data?.data ?? r.data),
  // Export lineage sub-graph as JSON (nodes + links) used to derive CSV export in frontend.
  exportLineage: (params) => api.get('/api/lineage/export', { params }).then(r => r.data?.data ?? r.data),
  // Fetch detail of a single lineage entity by type + id.
  getEntity: (entityType, id) => api.get(`/api/lineage/${enc(entityType)}/${enc(id)}`).then(r => r.data?.data ?? r.data)
}
