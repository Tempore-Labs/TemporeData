import axios from 'axios'

const API_BASE = ''

const api = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// Request interceptor: attach JWT token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('td_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, error => Promise.reject(error))

// Response interceptor: handle errors
api.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code !== 0 && data.code !== undefined) {
      return Promise.reject(new Error(data.msg || 'Request failed'))
    }
    return response
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('td_token')
      localStorage.removeItem('td_user')
      // Hash router: use the login route, not a non-existent /login.html
      if (!window.location.hash.startsWith('#/login')) {
        window.location.href = '/#/login'
      }
    }
    return Promise.reject(error)
  }
)

// Re-export from modules for backward compatibility
export { authApi } from './modules/auth'
export { dashboardApi } from './modules/dashboard'
export { workflowApi } from './modules/workflow'
export { datasourceApi } from './modules/datasource'
export { queryApi } from './modules/query'
export { catalogApi } from './modules/catalog'
export { ingestionApi } from './modules/ingestion'
export { syncApi } from './modules/sync'
export { qualityApi } from './modules/quality'
export { engineApi } from './modules/engine'
export { dataApiApi } from './modules/dataapi'
export { workflowLineageApi } from './modules/workflowLineage'
export { workflowImportApi } from './modules/workflowImport'
export { schedulerApi } from './modules/scheduler'
export { calendarApi } from './modules/calendar'
export { agentApi } from './modules/agent'
export { securityApi } from './modules/security'
export { opsApi } from './modules/ops'
export { permApi } from './modules/perm'
export { auditApi } from './modules/audit'
export { clusterApi } from './modules/cluster'
export { alarmApi } from './modules/alarm'
export { fileApi } from './modules/file'
export { funcApi } from './modules/func'

export { realtimeApi } from './modules/realtime'
export { secretApi } from './modules/secret'
export { tenantApi } from './modules/tenant'
export { metaApi } from './modules/meta'
export { workApi } from './modules/work'
export { monitorApi } from './modules/monitor'
export { haApi } from './modules/ha'
export { containerApi } from './modules/container'
export { viewApi } from './modules/view'
// New modules
export { globalvarApi } from './modules/globalvar'
export { userApi } from './modules/user'
export { datasourcePluginApi } from './modules/datasourcePlugin'
export { dependencyApi } from './modules/dependency'
export { resourceApi } from './modules/resource'
export { notifyApi } from './modules/notify'
export { messageApi } from './modules/message'
export { roleApi } from './modules/role'
export { orgApi } from './modules/org'
export { approvalApi } from './modules/approval'
export { indicatorApi } from './modules/indicator'
export { tagApi } from './modules/tag'
export { reportApi } from './modules/report'
export { formApi } from './modules/form'
export { preferenceApi } from './modules/preference'
export { apilogApi } from './modules/apilog'
export { blacklistApi } from './modules/blacklist'
export { sensitiveApi } from './modules/sensitive'
export { mydataApi } from './modules/mydata'
export { permapprovalApi } from './modules/permapproval'
export { datacenterApi } from './modules/datacenter'
export { passwordlessApi } from './modules/passwordless'
export { settingsApi } from './modules/settings'

export default api