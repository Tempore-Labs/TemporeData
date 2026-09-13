/** Incident API. */
import { apiGet, apiPost, apiPut } from './http'
import type { IncidentEntity } from '@/types/incident'

export const incidentApi = {
  open: (title: string, sourceType?: string, datasetId?: number, severity?: string, rca?: string) =>
    apiPost<IncidentEntity>(
      '/incident/open',
      { title, sourceType, datasetId, severity, rca },
      true,
    ),
  updateStatus: (id: number, status: string) =>
    apiPut<IncidentEntity>(`/incident/${id}/status`, { status }, true),
  setRca: (id: number, analysis: string) =>
    apiPut<IncidentEntity>(`/incident/${id}/rca`, { analysis }, true),
  get: (id: number) => apiGet<IncidentEntity>(`/incident/${id}`),
  byDataset: (datasetId: number) => apiGet<IncidentEntity[]>(`/incident/dataset/${datasetId}`),
  byStatus: (status: string) => apiGet<IncidentEntity[]>(`/incident/status/${status}`),
  openCount: () => apiGet<number>('/incident/open/count'),
}
