/**
 * data feature API — v2.0 migration.
 * Short-term: bridges the existing backend (datasourceApi, metaApi, lineageApi;
 * /api/datasource, /api/public/meta, /api/lineage) so the migrated pages keep
 * working today. When the /api/v1 contract lands, swap impls to the documented
 * endpoints without touching views (ACL — refactor principle).
 */
import { datasourceApi } from '@/api/modules/datasource'
import { metaApi } from '@/api/modules/meta'

export const listDatasources = () => datasourceApi.list()
export const testDatasource = (id) => datasourceApi.test(id)
export const listMetaOverview = () => metaApi.realOverview()

// ---- Future /api/v1 contract (v2 backend) ----
// listDatasources = () => http.get('/data/datasources')
// listMetaOverview = () => http.get('/data/metadata/overview')
// Lineage reuses the mature legacy lineageApi directly via the feature view.
// (reference: refactor/16-api-endpoint-matrix.md)