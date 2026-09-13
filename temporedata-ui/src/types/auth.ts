/** Auth module types (POST /api/auth/login|logout). */

export interface LoginReq {
  username: string
  password: string
}

export interface UserInfo {
  userId: string
  username: string
  tenantId?: string
  tenantName?: string
  roles: string[]
  permissions: string[]
  isAdmin: boolean
  nickname?: string
  phone?: string
}

export interface LoginRes {
  token: string
  user: UserInfo
}
