/**
 * Data-source brand-chip registry for lineage graph nodes.
 *
 * The lineage node data carries only a free-text `datasourceName`, so we map it
 * to a recognizable brand chip (color + short label + container shape) by
 * substring matching. Unknown sources fall back to an initial-letter chip so
 * every node still gets a stable, debuggable visual anchor.
 */

// [matchKeys, abbr, bg, fg, shape]
// shape ∈ 'circle' | 'hexagon' | 'rounded' | 'diamond'
const RULES = [
  // BigQuery
  [['bigquery', 'big query', 'bq'], 'BQ', '#3570ee', '#ffffff', 'hexagon'],
  // MySQL
  [['mysql'], 'MY', '#00758f', '#ffffff', 'rounded'],
  // MariaDB
  [['mariadb'], 'MA', '#003545', '#c0765a', 'rounded'],
  // PostgreSQL
  [['postgresql', 'postgres', 'pg'], 'PG', '#336791', '#ffffff', 'circle'],
  // Hive / HiveQL
  [['hive'], 'HV', '#fdbe02', '#222222', 'diamond'],
  // Doris
  [['doris'], 'DO', '#ffb02e', '#5b2d0c', 'diamond'],
  // StarRocks
  [['starrocks', 'star rocks'], 'SR', '#6c4ff6', '#ffffff', 'diamond'],
  // ClickHouse
  [['clickhouse', 'click house'], 'CH', '#ffcc01', '#281c08', 'rounded'],
  // Trino
  [['trino'], 'TR', '#dd00a1', '#ffffff', 'circle'],
  // Presto
  [['presto'], 'PR', '#3c82f6', '#ffffff', 'circle'],
  // Spark
  [['spark'], 'SP', '#e25a1c', '#ffffff', 'circle'],
  // Flink
  [['flink'], 'FK', '#e70b0b', '#ffffff', 'circle'],
  // Oracle
  [['oracle'], 'OR', '#f00000', '#ffffff', 'circle'],
  // SQL Server
  [['sql server', 'sqlserver', 'mssql', 'sql_server'], 'SQL', '#cc2927', '#ffffff', 'rounded'],
  // Power BI / PowerBI
  [['powerbi', 'power bi', 'power_bi'], 'PB', '#f2c811', '#231d07', 'rounded'],
  // Tableau
  [['tableau'], 'TB', '#e97627', '#ffffff', 'rounded'],
  // Elasticsearch
  [['elastic', 'es_', 'elasticsearch'], 'ES', '#005571', '#fec514', 'circle'],
  // HDFS / Hadoop
  [['hdfs', 'hadoop'], 'HD', '#ffd000', '#2c2c2c', 'diamond'],
  // Kafka
  [['kafka'], 'KF', '#231f20', '#ffffff', 'circle'],
  // Snowflake
  [['snowflake'], 'SF', '#29b5e8', '#ffffff', 'diamond'],
  // Redshift
  [['redshift'], 'RS', '#1b2634', '#5b8def', 'circle'],
  // Redis
  [['redis'], 'RD', '#dc382d', '#ffffff', 'circle']
]

const DEFAULT_BG = '#e2e8f0'
const DEFAULT_FG = '#475569'

/**
 * Resolve a brand chip descriptor for the given datasource name.
 * @param {string|null|undefined} datasourceName
 * @returns {{abbr:string,bg:string,fg:string,shape:string}|null}
 */
export function resolveSource(datasourceName) {
  const raw = (datasourceName || '').trim()
  if (!raw) return null
  const name = raw.toLowerCase()
  for (const [keys, abbr, bg, fg, shape] of RULES) {
    if (keys.some((k) => name.includes(k))) {
      return { abbr, bg, fg, shape }
    }
  }
  return { abbr: raw.charAt(0).toUpperCase() || '?', bg: DEFAULT_BG, fg: DEFAULT_FG, shape: 'rounded' }
}

/**
 * Render the SVG path for a brand chip container shape.
 * @param {string} shape
 * @param {string} color
 * @returns {string} an <svg viewBox="0 0 24 24"> markup fragment
 */
export function chipSvg(shape, color) {
  const fill = shadowChip(shape, color)
  return `<svg viewBox="0 0 24 24" width="1em" height="1em" aria-hidden="true">${fill}</svg>`
}

function shadowChip(shape, color) {
  switch (shape) {
    case 'hexagon': {
      const p = '12 2 21 7 21 17 12 22 3 17 3 7'
      const d = p.split(' ').reduce((acc, c, i) => {
        acc += (i % 2 === 0 ? (i === 0 ? 'M' : 'L') : ',') + c
        return acc
      }, '')
      return `<path d="${d}Z" fill="${color}"/>`
    }
    case 'diamond':
      return `<rect x="7.5" y="7.5" width="9" height="9" rx="1.6" transform="rotate(45 12 12)" fill="${color}"/>`
    case 'rounded':
      return `<rect x="2.5" y="5" width="19" height="14" rx="4" fill="${color}"/>`
    case 'circle':
    default:
      return `<circle cx="12" cy="12" r="9.5" fill="${color}"/>`
  }
}

/** Full SVG chip with the source abbreviation centered. */
export function chipMark(desc) {
  return {
    abbr: desc.abbr,
    bg: desc.bg,
    fg: desc.fg,
    shapeSvg: shadowChip(desc.shape, desc.bg)
  }
}