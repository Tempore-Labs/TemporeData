/** Compute cluster API. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'

export interface ClusterEntity {
  id: string
  name?: string
  type?: string
  host?: string
  port?: number
  username?: string
  password?: string
  masterUrl?: string
  status?: string
  agentStatus?: string
  nodeCount?: number
  cpuUsage?: number
  memoryUsage?: number
  diskUsage?: number
  createTime?: string
}

export const clusterApi = {
  list: () => apiGet<ClusterEntity[]>('/cluster'),
  get: (id: string) => apiGet<ClusterEntity>(`/cluster/${id}`),
  create: (d: Partial<ClusterEntity>) => apiPost<ClusterEntity>('/cluster', d),
  update: (id: string, d: Partial<ClusterEntity>) => apiPut<ClusterEntity>(`/cluster/${id}`, d),
  remove: (id: string) => apiDelete<void>(`/cluster/${id}`),
  nodes: (clusterId: string) => apiGet<Record<string, unknown>[]>(`/cluster/${clusterId}/nodes`),
  addNode: (clusterId: string, node: Record<string, unknown>) =>
    apiPost<Record<string, unknown>>(`/cluster/${clusterId}/nodes`, node),
  installAgent: (nodeId: string) => apiPost<Record<string, unknown>>(`/cluster/nodes/${nodeId}/install`),
  nodeStatus: (nodeId: string) => apiGet<Record<string, unknown>>(`/cluster/nodes/${nodeId}/status`),
}