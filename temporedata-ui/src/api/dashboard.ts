/** Dashboard API. */
import { apiGet } from './http'
import type { DashboardRes } from '@/types/dashboard'

export const dashboardApi = {
  overview: () => apiGet<DashboardRes>('/dashboard'),
}
