/** Permission center API. */
import { apiDelete, apiGet, apiPost } from './http'

export interface ResourceEntity {
  id: string
  resourceType?: string
  resourceKey?: string
  resourceName?: string
  tenantId?: string
  owner?: string
  description?: string
  createTime?: string
}

export interface PermissionEntity {
  id: string
  roleId?: string
  resourceType?: string
  resourceKey?: string
  action?: string
  scope?: string
  tenantId?: string
  createTime?: string
}

export const permApi = {
  resources: (resourceType?: string, name?: string) =>
    apiGet<ResourceEntity[]>('/perm/resources', { resourceType, name }),
  registerResource: (d: Partial<ResourceEntity> & { resourceType: string; resourceKey: string }) =>
    apiPost<ResourceEntity>('/perm/resources', d),
  deleteResource: (id: string) => apiDelete<void>(`/perm/resources/${id}`),
  permissions: (roleIds?: string) =>
    apiGet<PermissionEntity[]>('/perm/permissions', { roleIds }),
  grant: (d: Partial<PermissionEntity>) => apiPost<PermissionEntity>('/perm/permissions', d),
  revoke: (id: string) => apiDelete<void>(`/perm/permissions/${id}`),
  myPerms: (roleIds?: string) => apiGet<string[]>('/perm/my/perms', { roleIds }),
  verify: (body: Record<string, unknown>) => apiPost<boolean>('/perm/verify', body),
  accessible: (type: string, superAdmin = false, roleIds?: string) =>
    apiGet<ResourceEntity[]>(`/perm/resources/${type}/accessible`, { superAdmin, roleIds }),
}