// v2.0 navigation model (menu + route meta). Kept as data so Layout can render and
// Command Palette can search. Feature route tables live in `features/*/routes.js`.
export const NAV = {
  dashboard: { path: '/v2/dashboard', label: '总览', icon: 'Odometer', group: 'Overview' },
  clusters: { path: '/v2/clusters', label: '集群', icon: 'Monitor', group: 'Compute' },
  compute: { path: '/v2/compute', label: '计算', icon: 'Cpu', group: 'Compute' },
  jobs: { path: '/v2/jobs', label: '作业', icon: 'Tickets', group: 'Data Dev' },
  monitoring: { path: '/v2/monitoring', label: '监控', icon: 'DataLine', group: 'Ops' },
  logs: { path: '/v2/logs', label: '日志', icon: 'Document', group: 'Ops' },
  incidents: { path: '/v2/incidents', label: '事件', icon: 'Warning', group: 'Ops' },
  automation: { path: '/v2/automation/runbooks', label: '自动化', icon: 'MagicStick', group: 'Ops' },
  data: { path: '/v2/data/datasources', label: '数据', icon: 'Coin', group: 'Data' },
  admin: { path: '/v2/admin', label: '管理', icon: 'Setting', group: 'Admin' }
}