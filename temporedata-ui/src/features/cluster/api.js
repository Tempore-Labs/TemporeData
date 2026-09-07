/**
 * cluster feature API — v2.0 migration.
 * Short-term: bridges the existing backend (clusterApi, /api/cluster) so migrated
 * pages keep working today. When the /api/v1 contract lands, swap impls to the
 * documented endpoints without touching views (ACL — refactor principle).
 */
import { clusterApi } from '@/api/modules/cluster'

export const listClusters = () => clusterApi.list()
export const getCluster = (id) => clusterApi.detail(id)
export const createCluster = (data) => clusterApi.create(data)
export const updateCluster = (id, data) => clusterApi.update(id, data)
export const deleteCluster = (id) => clusterApi.delete(id)
export const listClusterNodes = (clusterId) => clusterApi.listNodes(clusterId)
export const addClusterNode = (clusterId, data) => clusterApi.addNode(clusterId, data)

// ---- Future /api/v1 contract (v2 backend) ----
// listClusters = () => http.get('/clusters')
// getCluster    = (id) => http.get(`/clusters/${id}`)
// createCluster = (data) => http.post('/clusters', data)
// listClusterNodes = (clusterId) => http.get(`/clusters/${clusterId}/nodes`)
// (reference: refactor/16-api-endpoint-matrix.md)