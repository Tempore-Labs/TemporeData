/**
 * Central navigation model: menu groups + route meta.
 * Consumed by the Sidebar and the Topbar breadcrumb.
 */
export interface NavItem {
  path: string
  label: string
  group: string
  icon?: string
}

export const NAV: NavItem[] = [
  // 数据资产
  { path: '/dashboard', label: '数据概览', group: '数据资产', icon: 'Odometer' },
  { path: '/cluster', label: '计算集群', group: '数据资产', icon: 'Monitor' },
  { path: '/container', label: '计算容器', group: '数据资产', icon: 'Box' },
  { path: '/resource', label: '资源中心', group: '数据资产', icon: 'Folder' },
  { path: '/datasource', label: '数据源管理', group: '数据资产', icon: 'Coin' },
  { path: '/catalog', label: '数据目录', group: '数据资产', icon: 'Coin' },
  { path: '/lineage', label: '数据血缘', group: '数据资产', icon: 'Share' },
  // 数据开发
  { path: '/sql-editor', label: 'SQL 编辑器', group: '数据开发', icon: 'Edit' },
  { path: '/workflow', label: '工作流管理', group: '数据开发', icon: 'Tickets' },
  { path: '/scheduler', label: '调度中心', group: '数据开发', icon: 'Timer' },
  // 数据运维
  { path: '/monitor', label: '监控总览', group: '数据运维', icon: 'DataLine' },
  { path: '/alarm', label: '告警配置', group: '数据运维', icon: 'Bell' },
  { path: '/audit', label: '审计中心', group: '数据运维', icon: 'Document' },
  // 系统管理
  { path: '/system', label: '系统管理', group: '系统管理', icon: 'Setting' },
  { path: '/perm', label: '权限中心', group: '系统管理', icon: 'Lock' },
  { path: '/permapproval', label: '权限审批', group: '系统管理', icon: 'Checked' },
  { path: '/mydata', label: '我的数据', group: '数据资产', icon: 'User' },
  { path: '/datacenter', label: '数据中心', group: '数据资产', icon: 'OfficeBuilding' },
  // 数据接入
  { path: '/ingestion', label: '数据接入', group: '数据开发', icon: 'Download' },
  { path: '/sync', label: '数据同步', group: '数据开发', icon: 'Refresh' },
  { path: '/work', label: '作业管理', group: '数据开发', icon: 'SetUp' },
  { path: '/realtime', label: '实时任务', group: '数据开发', icon: 'VideoPlay' },
  { path: '/approval', label: '审批中心', group: '系统管理', icon: 'Checked' },
  { path: '/file-center', label: '文件中心', group: '数据资产', icon: 'FolderOpened' },
  { path: '/meta', label: '元数据采集', group: '数据资产', icon: 'DataAnalysis' },
  { path: '/preference', label: '系统偏好', group: '系统管理', icon: 'Setting' },
  { path: '/view', label: '视图管理', group: '系统管理', icon: 'View' },
  { path: '/passwordless', label: '免密登录', group: '系统管理', icon: 'Unlock' },
  { path: '/engine', label: '计算引擎', group: '数据运维', icon: 'Cpu' },
  { path: '/ops-git', label: 'Git 运维', group: '数据运维', icon: 'Switch' },
  { path: '/func', label: '函数管理', group: '数据开发', icon: 'Crop' },
  { path: '/globalvar', label: '全局变量', group: '数据开发', icon: 'Collection' },
  { path: '/dependency', label: '依赖管理', group: '数据开发', icon: 'Box' },
  { path: '/apilog', label: 'API 日志', group: '数据服务', icon: 'Document' },
  { path: '/form', label: '动态表单', group: '数据服务', icon: 'EditPen' },
  // 数据治理
  { path: '/sensitive', label: '敏感数据', group: '数据治理', icon: 'Lock' },
  { path: '/security', label: '安全治理', group: '数据治理', icon: 'Key' },
  { path: '/data-service', label: '接口服务', group: '数据服务', icon: 'Connection' },
  { path: '/report', label: '数据报表', group: '数据服务', icon: 'Document' },
  { path: '/blacklist', label: '黑白名单', group: '数据服务', icon: 'List' },
  { path: '/ha', label: '高可用', group: '数据运维', icon: 'Connection' },
  { path: '/tag', label: '标签与指标', group: '数据治理', icon: 'PriceTag' },
  { path: '/quality', label: '质量管理', group: '数据治理', icon: 'Aim' },
  { path: '/datasets', label: '数据集管理', group: '数据治理', icon: 'Coin' },
  { path: '/asset', label: '资产指标', group: '数据治理', icon: 'Box' },
  // 企业模块
  { path: '/contract', label: '数据契约', group: '企业模块', icon: 'DocumentChecked' },
  { path: '/gateway', label: '网关控制台', group: '企业模块', icon: 'Connection' },
  { path: '/cost', label: '成本治理', group: '企业模块', icon: 'Money' },
  // 企业运维
  { path: '/incidents', label: '事件闭环', group: '企业运维', icon: 'Warning' },
  { path: '/ai-safety', label: 'AI 门控', group: '企业运维', icon: 'Lock' },
  { path: '/rca', label: 'RCA-Agent', group: '企业运维', icon: 'MagicStick' },
]

export const NAV_GROUPS: string[] = [...new Set(NAV.map((n) => n.group))]

export function navLabel(path: string): string {
  const hit = NAV.find((n) => n.path === path)
  return hit?.label || path
}
