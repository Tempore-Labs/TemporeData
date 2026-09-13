/** System management API: user / role / tenant / org. */
import { apiDelete, apiGet, apiPost, apiPut } from './http'

export interface UserRes {
  id: string
  username: string
  tenantId?: string
  status?: number
  density?: string
  mustChangePassword?: boolean
  nickname?: string
  phone?: string
  email?: string
  createdAt?: string
}

export interface RoleEntity {
  id: string
  code: string
  name: string
  remark?: string
  protectedRole?: boolean
  dataScope?: string
  createTime?: string
}

export interface TenantEntity {
  id: string
  name?: string
  code?: string
  description?: string
  status?: number
  contactName?: string
  contactEmail?: string
  memberCount?: number
  createTime?: string
}

export interface OrgEntity {
  id: string
  username?: string
  nickname?: string
  createTime?: string
}

export const sysApi = {
  // users
  users: () => apiGet<UserRes[]>('/user'),
  user: (id: string) => apiGet<UserRes>(`/user/${id}`),
  createUser: (d: Partial<UserRes> & { password?: string }) => apiPost<UserRes>('/user', d),
  updateUser: (id: string, d: Partial<UserRes>) => apiPut<UserRes>(`/user/${id}`, d),
  deleteUser: (id: string) => apiDelete<void>(`/user/${id}`),
  setUserRoles: (id: string, roleIds: string[]) =>
    apiPut<void>(`/user/${id}/roles`, roleIds),

  // roles
  roles: () => apiGet<RoleEntity[]>('/role'),
  createRole: (d: Partial<RoleEntity>) => apiPost<RoleEntity>('/role', d),
  updateRole: (id: string, d: Partial<RoleEntity>) => apiPut<RoleEntity>(`/role/${id}`, d),
  deleteRole: (id: string) => apiDelete<void>(`/role/${id}`),
  roleMembers: (id: string) => apiGet<Record<string, unknown>[]>(`/role/${id}/members`),

  // tenants
  tenants: () => apiGet<TenantEntity[]>('/tenant'),
  createTenant: (d: Partial<TenantEntity>) => apiPost<TenantEntity>('/tenant', d),
  updateTenant: (id: string, d: Partial<TenantEntity>) => apiPut<TenantEntity>(`/tenant/${id}`, d),
  deleteTenant: (id: string) => apiDelete<void>(`/tenant/${id}`),

  // orgs
  orgs: () => apiGet<OrgEntity[]>('/org/list'),
  createOrg: (d: Partial<OrgEntity>) => apiPost<OrgEntity>('/org', d),
  updateOrg: (d: Partial<OrgEntity>) => apiPut<OrgEntity>('/org', d),
  deleteOrg: (id: string) => apiDelete<void>(`/org/${id}`),
}