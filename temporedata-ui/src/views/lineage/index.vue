<template>
  <div class="lineage-page">
    <!-- Toolbar -->
    <div class="toolbar">
      <el-input v-model="kw" placeholder="搜索节点..." size="small" clearable style="width: 180px" @keyup.enter="onSearch" @clear="onSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button size="small" @click="loadGraph">重建</el-button>
      <el-button size="small" @click="overviewVisible = true">总览</el-button>

      <div class="sep" />

      <span class="tb-label">粒度</span>
      <el-radio-group v-model="graphLevel" size="small">
        <el-radio-button value="table">表级</el-radio-button>
        <el-radio-button value="column" disabled>字段级</el-radio-button>
      </el-radio-group>

      <div class="sep" />

      <span class="tb-label">域</span>
      <el-select v-model="domainFilter" size="small" style="width: 110px" clearable placeholder="全部">
        <el-option v-for="d in domains" :key="d" :label="d" :value="d" />
      </el-select>

      <div class="flex-1" />

      <div class="ws-chip">
        <span class="ws-dot" />
        API 已连接
      </div>
    </div>

    <!-- Body -->
    <div class="body">
      <!-- Asset tree -->
      <div v-if="!treeCollapsed" class="tree-pane">
        <div class="tree-head">
          <span>资产树</span>
          <button class="collapse-btn" title="收起" @click="treeCollapsed = true">
            <el-icon :size="12"><Fold /></el-icon>
          </button>
        </div>
        <div class="tree-scroll">
          <el-skeleton v-if="graphLoading" :rows="8" animated />
          <template v-else>
            <div v-for="d in domains" :key="d" class="tree-group">
              <div class="tree-domain">▾ {{ d }}</div>
              <button
                v-for="n in nodesOfDomain(d)"
                :key="n.id"
                class="tree-node"
                :class="{ active: selectedId === n.id }"
                @click="selectedId = n.id"
              >
                <span class="tree-badge" :style="badgeStyle(n)">{{ badgeText(n) }}</span>
                <span class="mono tree-name">{{ shortName(n) }}</span>
              </button>
            </div>
            <div v-if="!nodes.length" class="tree-empty">无节点，点击"重建"从图接口加载</div>
          </template>
        </div>
      </div>

      <button v-else class="tree-collapsed-btn" title="展开资产树" @click="treeCollapsed = false">
        <el-icon :size="12"><Expand /></el-icon>
      </button>

      <!-- Canvas -->
      <LineageCanvas
        :nodes="nodes"
        :links="links"
        :selected-id="selectedId"
        :search-match="searchMatch"
        @select="selectedId = $event"
      />

      <!-- Detail panel -->
      <aside class="detail-pane">
        <template v-if="selectedNode">
          <div class="dp-head">
            <div class="dp-row">
              <span class="dp-badge" :style="badgeStyle(selectedNode)">{{ badgeText(selectedNode) }}</span>
              <button class="dp-close" @click="selectedId = null">✕</button>
            </div>
            <div class="mono dp-name">{{ selectedNode.label || selectedNode.name }}</div>
            <div class="dp-schema">Schema: <span class="mono">{{ selectedNode.schema || '—' }}</span></div>
          </div>

          <div class="dp-block">
            <div class="dp-block-title">资产信息</div>
            <div v-for="[k, v] in assetMeta" :key="k" class="dp-kv">
              <span class="dp-k">{{ k }}</span>
              <span class="dp-v">{{ v }}</span>
            </div>
          </div>

          <div v-if="selectedNode.cols?.length" class="dp-block dp-cols">
            <div class="dp-block-title">字段列表 ({{ selectedNode.cols.length }})</div>
            <div v-for="(c, i) in selectedNode.cols" :key="c" class="dp-col">
              <span class="dp-col-badge">{{ i === 0 ? 'PK' : 'COL' }}</span>
              <span class="mono">{{ c }}</span>
            </div>
          </div>

          <div class="dp-block">
            <div class="dp-block-title">上下游关系</div>
            <div class="dp-subtitle">上游 ({{ upstream.length }})</div>
            <button v-for="n in upstream" :key="n.id" class="dp-neighbor" @click="selectedId = n.id">
              <span class="dp-dot" style="background: #2563eb" />
              <span class="mono">{{ n.label || n.name }}</span>
            </button>
            <div class="dp-subtitle">下游 ({{ downstream.length }})</div>
            <button v-for="n in downstream" :key="n.id" class="dp-neighbor" @click="selectedId = n.id">
              <span class="dp-dot" style="background: #10b981" />
              <span class="mono">{{ n.label || n.name }}</span>
            </button>
          </div>
        </template>
        <div v-else class="dp-empty">
          <div class="dp-empty-icon"><el-icon :size="26"><Share /></el-icon></div>
          <div>点击节点查看详情</div>
          <div class="dp-empty-sub">支持查看资产信息、字段列表和上下游关系</div>
        </div>
      </aside>
    </div>

    <!-- Overview dialog -->
    <el-dialog v-model="overviewVisible" title="血缘总览" width="420px">
      <el-descriptions v-if="overview" :column="1" border size="small">
        <el-descriptions-item label="节点总数">{{ overview.totalNodes }}</el-descriptions-item>
        <el-descriptions-item label="关系总数">{{ overview.totalEdges }}</el-descriptions-item>
        <el-descriptions-item label="最大深度">{{ overview.maxDepth }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无总览数据" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import LineageCanvas from '@/components/lineage/LineageCanvas.vue'
import { lineageApi } from '@/api/lineage'
import type { LineageGraph, LineageNode, LineageOverview } from '@/types/lineage'

const route = useRoute()
const kw = ref('')
const graphLevel = ref('table')
const domainFilter = ref<string | undefined>(undefined)
const treeCollapsed = ref(false)
const overviewVisible = ref(false)
const overview = ref<LineageOverview | null>(null)

const nodes = ref<LineageNode[]>([])
const links = ref<any[]>([])
const selectedId = ref<string | null>(null)
const searchMatch = ref<string[] | null>(null)
const graphLoading = ref(false)
const rootId = ref<string>('')

const TYPE_COLORS: Record<string, { bg: string; border: string; text: string; badge: string }> = {
  table: { bg: '#f0f9ff', border: '#2563eb', text: '#1e40af', badge: 'Table' },
  view: { bg: '#f0fdf4', border: '#10b981', text: '#065f46', badge: 'View' },
  kafka: { bg: '#fef3c7', border: '#f59e0b', text: '#92400e', badge: 'Kafka' },
  api: { bg: '#faf5ff', border: '#8b5cf6', text: '#5b21b6', badge: 'API' },
}

const domains = computed(() => [...new Set(nodes.value.map((n) => domainOf(n)))])
const nodesOfDomain = (d: string) => nodes.value.filter((n) => domainOf(n) === d)
const selectedNode = computed(() => nodes.value.find((n) => n.id === selectedId.value) || null)

const upstream = computed(() => {
  if (!selectedId.value) return []
  const src = links.value.filter((e) => e.target === selectedId.value).map((e) => e.source)
  return nodes.value.filter((n) => src.includes(n.id))
})
const downstream = computed(() => {
  if (!selectedId.value) return []
  const tgt = links.value.filter((e) => e.source === selectedId.value).map((e) => e.target)
  return nodes.value.filter((n) => tgt.includes(n.id))
})

const assetMeta = computed(() => {
  const n = selectedNode.value
  if (!n) return []
  return [
    ['Owner', n.owner || '—'],
    ['数据域', n.domain || '—'],
    ['类型', (TYPE_COLORS[(n.type || 'table') as string]?.badge) || n.type || '—'],
    ['更新时间', '—'],
  ] as [string, string][]
})

function badgeText(n: LineageNode) {
  const c = TYPE_COLORS[(n.type || 'table') as string] || TYPE_COLORS.table
  return c.badge[0]
}
function badgeStyle(n: LineageNode) {
  const c = TYPE_COLORS[(n.type || 'table') as string] || TYPE_COLORS.table
  return { background: c.bg, color: c.text }
}
function shortName(n: LineageNode) {
  const label = n.label || n.name || n.id
  return label.split('.').pop()
}

async function loadGraph() {
  graphLoading.value = true
  try {
    const g: LineageGraph = await lineageApi.graph(rootId.value || '1', 3)
    nodes.value = (g.nodes || []).map((n) => ({
      ...n,
      label: n.label || n.name,
      type: (n.type || n.nodeType || 'table').toLowerCase(),
    }))
    links.value = g.links || g.edges || []
    if (g.rootTableName && !rootId.value) {
      const root = g.nodes.find((n) => n.name === g.rootTableName || n.label === g.rootTableName)
      if (root) rootId.value = root.id
    }
    searchMatch.value = null
  } catch (err: any) {
    ElMessage.error(`血缘图加载失败: ${err?.message || ''}`)
  } finally {
    graphLoading.value = false
  }
}

/** Derive a meaningful asset-tree group from the backend node id (tbl:/task:/...). */
function domainOf(n: LineageNode): string {
  if (n.domain) return n.domain
  const id = String(n.id)
  if (id.startsWith('task:')) return '任务'
  if (id.startsWith('tbl:')) return '数据表'
  if (id.startsWith('kafka:')) return '消息'
  return '未分类'
}

/** Resolve a sensible root node id when none was requested. */
async function resolveRoot(): Promise<string> {
  if (rootId.value) return rootId.value
  try {
    const items = await lineageApi.search('a')
    const first = items.find((i) => /TABLE|VIEW/i.test(i.nodeType)) || items[0]
    if (first) return first.nodeId
  } catch {
    /* fall through */
  }
  return '1'
}

async function onSearch() {
  const q = kw.value.trim()
  if (!q) {
    searchMatch.value = null
    return
  }
  try {
    const items = await lineageApi.search(q)
    searchMatch.value = items.map((i) => i.nodeId)
    if (items.length) {
      // jump to the first match if the graph contains it
      const first = nodes.value.find((n) => n.id === items[0].nodeId)
      if (first) selectedId.value = first.id
    }
  } catch {
    searchMatch.value = null
  }
}

onMounted(async () => {
  // Allow navigation with ?kw=... (Topbar global search).
  const q = route.query.kw as string | undefined
  if (q) {
    kw.value = q
    rootId.value = await resolveRoot()
    await loadGraph()
    await onSearch()
  } else {
    rootId.value = await resolveRoot()
    await loadGraph()
  }
  try {
    overview.value = await lineageApi.overview()
  } catch {
    /* optional */
  }
})

watch(selectedId, (id) => {
  if (id && !nodes.value.find((n) => n.id === id)) return
  searchMatch.value = null
})
</script>

<style scoped>
.lineage-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--td-bg);
}
.toolbar {
  background: var(--td-surface);
  border-bottom: 1px solid var(--td-border);
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  flex-shrink: 0;
}
.sep {
  width: 1px;
  height: 20px;
  background: var(--td-border);
}
.tb-label {
  font-size: 12px;
  color: var(--td-text-3);
}
.flex-1 {
  flex: 1;
}
.ws-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 8px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  font-size: 10px;
  color: #065f46;
  font-weight: 500;
}
.ws-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.body {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

.tree-pane {
  width: 220px;
  background: var(--td-surface);
  border-right: 1px solid var(--td-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.tree-head {
  padding: 10px 12px;
  border-bottom: 1px solid var(--td-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
  color: var(--td-text-2);
}
.collapse-btn {
  border: none;
  background: transparent;
  color: var(--td-text-4);
  cursor: pointer;
  padding: 2px;
}
.tree-scroll {
  flex: 1;
  overflow: auto;
  padding: 8px;
}
.tree-group {
  margin-bottom: 4px;
}
.tree-domain {
  padding: 6px 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--td-text-2);
}
.tree-node {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
  font-size: 12px;
  color: var(--td-text-2);
}
.tree-node:hover {
  background: var(--td-border-light);
}
.tree-node.active {
  background: var(--td-primary-soft);
  color: var(--td-primary);
}
.tree-badge {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  font-size: 9px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.tree-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}
.tree-empty {
  padding: 12px;
  font-size: 11px;
  color: var(--td-text-4);
}
.tree-collapsed-btn {
  width: 32px;
  background: var(--td-surface);
  border: none;
  border-right: 1px solid var(--td-border);
  color: var(--td-text-3);
  cursor: pointer;
  flex-shrink: 0;
}
.tree-collapsed-btn:hover {
  background: var(--td-border-light);
}

.detail-pane {
  width: 280px;
  background: var(--td-surface);
  border-left: 1px solid var(--td-border);
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.dp-head {
  padding: 12px 16px;
  border-bottom: 1px solid var(--td-border-light);
}
.dp-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.dp-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}
.dp-close {
  border: none;
  background: transparent;
  color: var(--td-text-4);
  cursor: pointer;
  font-size: 12px;
}
.dp-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--td-text-1);
  word-break: break-all;
}
.dp-schema {
  font-size: 11px;
  color: var(--td-text-3);
  margin-top: 2px;
}
.dp-block {
  padding: 12px 16px;
  border-bottom: 1px solid var(--td-border-light);
  overflow-y: auto;
}
.dp-block-title {
  font-size: 10px;
  font-weight: 600;
  color: var(--td-text-4);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 8px;
}
.dp-kv {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  border-bottom: 1px solid var(--td-border-light);
  font-size: 12px;
}
.dp-kv:last-child {
  border-bottom: none;
}
.dp-k {
  color: var(--td-text-4);
}
.dp-v {
  color: var(--td-text-1);
  font-weight: 500;
}
.dp-cols {
  flex: 1;
}
.dp-col {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
  border-bottom: 1px solid var(--td-border-light);
  font-size: 12px;
}
.dp-col:last-child {
  border-bottom: none;
}
.dp-col-badge {
  font-size: 9px;
  padding: 1px 4px;
  border-radius: 4px;
  background: var(--td-border-light);
  color: var(--td-text-3);
  font-family: var(--td-font-mono);
  flex-shrink: 0;
}
.dp-subtitle {
  font-size: 10px;
  color: var(--td-text-3);
  margin: 6px 0 4px;
}
.dp-neighbor {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
  font-size: 12px;
}
.dp-neighbor:hover {
  background: var(--td-border-light);
}
.dp-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dp-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  text-align: center;
  color: var(--td-text-2);
  font-size: 13px;
}
.dp-empty-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--td-border-light);
  color: var(--td-text-3);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.dp-empty-sub {
  font-size: 11px;
  color: var(--td-text-4);
  margin-top: 4px;
}
.mono {
  font-family: var(--td-font-mono);
}
</style>
