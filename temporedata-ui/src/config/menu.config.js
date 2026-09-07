/**
 * Sidebar menu configuration (v2.0 information architecture).
 * 9 functional groups -> 7 workspace domains + Admin center + Personal space.
 * Paths keep existing routes so pages are reused without rewriting.
 *
 * `perm`（可选，P3-11）：菜单可见所需的最小权限点 `{resourceType}:{action}`，
 * 无 `perm` 表示不设权限门槛；超级管理员恒可见全部。
 */
export const menuConfig = [
  {
    group: '数据开发',
    icon: 'EditPen',
    children: [
      { path: '/workflow', title: '工作流', icon: 'Share' },
      { path: '/sqleditor', title: 'SQL 查询', icon: 'Document', perm: 'sqleditor:read' },
      { path: '/realtime', title: '实时计算', icon: 'Odometer' },
      { path: '/viewpage', title: '实时大屏', icon: 'DataBoard' },
      { path: '/globalvar', title: '全局变量', icon: 'PriceTag' },
      { path: '/funcrepo', title: '函数仓库', icon: 'MagicStick' },
      { path: '/dependency', title: '依赖合集', icon: 'Connection' },
      { path: '/approval', title: '发布中心', icon: 'DocumentChecked' }
    ]
  },
  {
    group: '数据资产',
    icon: 'Collection',
    children: [
      { path: '/dataasset', title: '数据目录', icon: 'MapLocation' },
      { path: '/datasource', title: '数据源', icon: 'Coin' },
      { path: '/meta', title: '元数据', icon: 'Upload' },
      { path: '/datacenter', title: '数据中心', icon: 'DataAnalysis' },
      { path: '/indicator', title: '指标与标签', icon: 'TrendCharts' },
      { path: '/tag', title: '数据标签', icon: 'PriceTag' },
      { path: '/qualityrule', title: '数据质量', icon: 'CircleCheck' },
      { path: '/lineage', title: '血缘分析', icon: 'Share' }
    ]
  },
  {
    group: '数据服务',
    icon: 'Link',
    children: [
      { path: '/dataapi', title: 'API 管理', icon: 'Link' },
      { path: '/report', title: '数据报表', icon: 'Document' },
      { path: '/form', title: '表单管理', icon: 'List' },
      { path: '/apilog', title: '服务日志', icon: 'Tickets' },
      { path: '/blacklist', title: '黑白名单', icon: 'Switch' }
    ]
  },
  {
    group: '运行中心',
    icon: 'TrendCharts',
    children: [
      { path: '/monitor', title: '运行总览', icon: 'DataBoard' },
      { path: '/scheduler', title: '调度任务', icon: 'Clock' },
      { path: '/calendar', title: '业务日历', icon: 'Calendar' },
      { path: '/alarm', title: '告警配置', icon: 'Bell' },
      { path: '/baseline', title: '基线告警', icon: 'Bell' },
      { path: '/ops', title: '集成配置', icon: 'Connection' }
    ]
  },
  {
    group: '安全治理',
    icon: 'Lock',
    children: [
      { path: '/perm', title: '权限中心', icon: 'Lock', perm: 'perm:read' },
      { path: '/role', title: '角色授权', icon: 'UserFilled', perm: 'role:read' },
      { path: '/mydata', title: '我的数据', icon: 'User' },
      { path: '/permapproval', title: '权限审批', icon: 'DocumentChecked', perm: 'permapproval:read' },
      { path: '/datalevel', title: '分类分级', icon: 'Flag' },
      { path: '/sensitive', title: '敏感数据', icon: 'Warning' },
      { path: '/datamask', title: '数据脱敏', icon: 'Hide' },
      { path: '/auditlog', title: '审计日志', icon: 'Tickets', perm: 'audit:read' },
      { path: '/changeaudit', title: '变更记录', icon: 'Clock' },
      { path: '/audit-center', title: '审计中心', icon: 'DocumentChecked', perm: 'audit:read' },
      { path: '/security-gov', title: '安全治理', icon: 'Warning', perm: 'security:read' }
    ]
  },
  {
    group: '平台资源',
    icon: 'Monitor',
    children: [
      { path: '/cluster', title: '计算集群', icon: 'Monitor' },
      { path: '/container', title: '计算容器', icon: 'Box' },
      { path: '/resource', title: '资源中心', icon: 'FolderOpened' }
    ]
  },
  {
    group: '管理中心',
    icon: 'Setting',
    admin: true,
    children: [
      { path: '/tenant', title: '租户与成员', icon: 'OfficeBuilding' },
      { path: '/org', title: '组织架构', icon: 'Share' },
      { path: '/passwordless', title: '免密登录', icon: 'Key' },
      { path: '/notify', title: '通知配置', icon: 'Bell' },
      { path: '/settings', title: '系统配置', icon: 'Setting' }
    ]
  },
  {
    group: '个人空间',
    icon: 'User',
    children: [
      { path: '/profile', title: '个人中心', icon: 'User' },
      { path: '/message', title: '消息中心', icon: 'Message' }
    ]
  }
]

// Flat menu list for quick lookup
export const flatMenus = menuConfig.flatMap(g => g.children)

// Icon name to component mapping
export const menuIcons = [
  'ChatDotRound', 'Monitor', 'Coin', 'Setting', 'Box', 'FolderOpened',
  'Layers', 'Grid', 'Stamp', 'Collection',
  'EditPen', 'Share', 'Odometer', 'PriceTag', 'MagicStick', 'Connection', 'DocumentChecked',
  'TrendCharts', 'Clock', 'Bell',
  'DataAnalysis', 'DataBoard', 'Upload',
  'CircleCheck', 'DocumentChecked',
  'Lock', 'Tickets', 'Flag', 'Warning', 'User', 'UserFilled',
  'MapLocation', 'Link', 'Switch', 'Document', 'List',
  'Reading', 'House', 'OfficeBuilding', 'Key', 'Message', 'Hide'
]