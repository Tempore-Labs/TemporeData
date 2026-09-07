/**
 * Lineage module i18n dictionary (Simplified Chinese). Default locale.
 * Keys follow 06 §6.3.2. Consumed by the lightweight `t(key)` helper in
 * LineageV2 that reads localStorage `td_locale`; no vue-i18n dependency.
 */
export default {
  'lineage.title': '血缘分析',
  'lineage.toolbar.rootSearch.placeholder': '搜索根节点（按资产名/ FQN）',
  'lineage.toolbar.level.table': '表级',
  'lineage.toolbar.level.column': '字段级',
  'lineage.toolbar.filter.domain': 'Domain',
  'lineage.toolbar.filter.owner': 'Owner',
  'lineage.toolbar.filter.tag': 'Tag',
  'lineage.toolbar.filter.column': 'Column',
  'lineage.toolbar.mode.dim': 'Dim',
  'lineage.toolbar.mode.hide': 'Hide',
  'lineage.toolbar.dir.both': '双向',
  'lineage.toolbar.dir.up': '仅上游',
  'lineage.toolbar.dir.down': '仅下游',
  'lineage.toolbar.layout.force': '力导向',
  'lineage.toolbar.layout.layer': '分层',
  'lineage.toolbar.layout.radial': '径向',
  'lineage.toolbar.cycleDetect': '环检测',
  'lineage.toolbar.actions.path': '路径分析',
  'lineage.toolbar.actions.heat': '热度排行',
  'lineage.toolbar.actions.sql': 'SQL 解析',
  'lineage.toolbar.actions.addEdge': '添加血缘',
  'lineage.toolbar.export.label': '导出',
  'lineage.toolbar.export.png': '导出 PNG',
  'lineage.toolbar.export.svg': '导出 SVG',
  'lineage.toolbar.export.csv': '导出 CSV',
  'lineage.status.connected': '实时连接',
  'lineage.status.offline': '离线',
  'lineage.dp.breadcrumb': '数据资产 / 血缘分析 / {name}',
  'lineage.dp.section.columns': '字段（{count}）',
  'lineage.dp.section.neighbors': '关联节点',
  'lineage.dp.neighbors.add': '＋ 添加关联节点',
  'lineage.dialog.addEdge.title': '新建血缘关系',
  'lineage.dialog.editEdge.title': '编辑血缘关系',
  'lineage.dialog.assetEdge.title': '从资产库添加血缘',
  'lineage.perm.writeRequired': '缺少 LINEAGE:WRITE 权限'
}