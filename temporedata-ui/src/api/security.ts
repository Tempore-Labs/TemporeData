/** Security governance API (P3-13 supply-chain & QA). */
import { apiGet, apiPost } from './http'

export interface VulnEntity {
  id: string
  source?: string
  packageName?: string
  cve?: string
  cvss?: number
  severity?: string
  affectedVer?: string
  fixedVer?: string
  status?: string
  prRef?: string
  mitigation?: string
  reportTime?: string
}

export interface QaReportEntity {
  id: string
  version?: string
  module?: string
  kind?: string
  summary?: string
  coverage?: number
  gate?: string
  runTime?: string
}

export interface GateCheck {
  criticalOpen: number
  highOpen: number
  gate: string
  reports?: { module: string; gate: string; coverage: number }[]
}

export const securityApi = {
  vulns: () => apiGet<VulnEntity[]>('/security/vulns'),
  createVuln: (d: Partial<VulnEntity>) => apiPost<VulnEntity>('/security/vulns', d),
  updateVulnStatus: (id: string, status: string) =>
    apiPost<VulnEntity>(`/security/vulns/${id}/status`, { status }),
  acceptVuln: (id: string, mitigation?: string) =>
    apiPost<VulnEntity>(`/security/vulns/${id}/accept`, { mitigation }),
  sbom: (version?: string) => apiGet<Record<string, unknown>>('/security/sbom', { version }),
  gate: () => apiGet<GateCheck>('/security/gate'),
  qa: (version?: string) => apiGet<QaReportEntity[]>('/security/qa', { version }),
  addQa: (d: Partial<QaReportEntity>) => apiPost<QaReportEntity>('/security/qa', d),
}