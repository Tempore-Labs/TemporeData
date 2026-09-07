/**
 * admin feature API — v2.0 migration.
 * Short-term: bridges the existing backend (tenantApi, orgApi; /api/tenant,
 * /api/org) so the migrated landing page keeps working today. When the /api/v1
 * contract lands, swap impls to the documented endpoints without touching views
 * (ACL — refactor principle).
 */
import { tenantApi } from '@/api/modules/tenant'
import { orgApi } from '@/api/modules/org'

export const listTenants = () => tenantApi.list()
export const listOrgTree = () => orgApi.tree()

// ---- Future /api/v1 contract (v2 backend) ----
// listTenants  = () => http.get('/admin/tenants')
// listOrgTree  = () => http.get('/admin/orgs/tree')
// (reference: refactor/16-api-endpoint-matrix.md)