import api from '../index'

/**
 * Metadata management API — wired to the REAL collected metadata
 * (zy_meta_table / zy_meta_column) and real registration-driven collection.
 * The legacy /api/meta* mock (MetaService/MetaEntity) has been removed.
 */
export const metaApi = {
  // ---- real metadata collection (registered datasources) ----
  collectByDatasource: (datasourceId) =>
    api.post(`/api/public/meta/collect-ds/${datasourceId}`).then(r => r.data.data),
  collectAllReal: () =>
    api.post('/api/public/meta/collect-all').then(r => r.data.data),
  realTables: (datasourceId) =>
    api.get('/api/public/meta/tables', { params: datasourceId ? { datasourceId } : {} }).then(r => r.data.data),
  realColumnsByDatasource: (datasourceId) =>
    api.get('/api/public/meta/columns', { params: { datasourceId } }).then(r => r.data.data),
  realOverview: () =>
    api.get('/api/public/meta/overview').then(r => r.data.data)
}