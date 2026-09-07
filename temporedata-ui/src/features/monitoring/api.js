/**
 * monitoring feature API — v2.0 migration.
 * Short-term: bridges the existing backend (monitorApi, /api/monitor) so the migrated
 * page keeps working today. When the /api/v1 contract lands, swap impls to the
 * documented endpoints without touching views (ACL — refactor principle).
 * Contract reference: refactor/16-api-endpoint-matrix.md (metrics:read).
 */
import { monitorApi } from '@/api/modules/monitor'
import { clusterApi } from '@/api/modules/cluster'

export const getSystemMetric = () => monitorApi.system()
export const getInstanceMetric = () => monitorApi.instance()
export const listMonitorClusters = () => clusterApi.list()
export const getClusterMetric = (clusterId) => monitorApi.cluster(clusterId)

// ---- Future /api/v1 contract (v2 backend) ----
// getSystemMetric     = () => http.get('/metrics', { params: { scope: 'system' } })
// getInstanceMetric   = () => http.get('/metrics', { params: { scope: 'instance' } })
// listMonitorClusters = () => http.get('/clusters')
// getClusterMetric    = (clusterId) => http.get('/metrics', { params: { scope: 'cluster', clusterId } })