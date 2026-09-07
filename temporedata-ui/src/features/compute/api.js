/**
 * compute feature API — v2.0 migration.
 * Short-term: bridges the existing backend (containerApi, engineApi, /api/container
 * and /api/engine) so the migrated page keeps working today. When the /api/v1
 * contract lands, swap impls to the documented endpoints without touching views
 * (ACL — refactor principle).
 */
import { containerApi } from '@/api/modules/container'
import { engineApi } from '@/api/modules/engine'

export const listComputeContainers = () => containerApi.list()
export const listEngineSpark = () => engineApi.listSpark()
export const listEngineFlink = () => engineApi.listFlink()

// ---- Future /api/v1 contract (v2 backend) ----
// listComputeContainers = () => http.get('/computes/containers')
// listEngineSpark       = () => http.get('/computes/engine/spark')
// listEngineFlink       = () => http.get('/computes/engine/flink')
// (reference: refactor/16-api-endpoint-matrix.md)