/** Data lineage API. */
import { apiGet } from './http'
import type { LineageGraph, LineageOverview, LineageSearchItem } from '@/types/lineage'

export const lineageApi = {
  graph: (nodeId: string, maxDepth = 3) =>
    apiGet<LineageGraph>('/lineage/graph', { nodeId, maxDepth }),
  search: (kw: string) => apiGet<LineageSearchItem[]>('/lineage/search', { keyword: kw }),
  overview: () => apiGet<LineageOverview>('/lineage/overview'),
}
