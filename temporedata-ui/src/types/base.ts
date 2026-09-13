/**
 * Base API envelope types shared across all backend endpoints.
 * Backend wraps every response in `BaseResponse<T>{code,msg,data}`.
 */
export interface BaseResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

/** Spring Data `Page<T>` JSON shape (serialized by Spring Boot). */
export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  numberOfElements?: number
  first?: boolean
  last?: boolean
  empty?: boolean
}

/** Common page query params for GET /xxx/page endpoints. */
export interface PageQuery {
  page?: number
  size?: number
  [k: string]: unknown
}
