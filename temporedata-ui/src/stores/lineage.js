import { defineStore } from 'pinia'
import { lineageApi } from '@/api/modules/lineage'

/**
 * Lineage module state: graph data, root node, filters, layout, selection.
 */
export const useLineageStore = defineStore('lineage', {
  state: () => ({
    rootNodeId: null,
    isOverview: false,
    graphData: null,
    selectedNode: null,
    layoutMode: 'layer',
    graphLevel: 'table',
    graphDirection: 'both',
    graphDepth: 3,
    nodeTypeFilter: null,
    graphLoading: false,
    nodeNameMap: {},
    wsEvent: null,
    // Visibility layer toggles: entity (base nodes), column (field links), dq (data-quality heat).
    layers: { entity: true, column: false, dq: false },
    // Manual node position overrides applied on top of the computed layout.
    // Stored as a plain reactive object (not Map) because Pinia serialization prefers plain objects.
    // Shape: { [nodeId]: { x, y } }
    layoutOverrides: {},
    // Edge IDs that should be visually highlighted because their field-links match the active column.
    // Kept as array (not reactive Set) for reliable serialization / devtools support.
    highlightedFieldEdgeIds: [],
    // Cached data-quality scores by nodeId. Values are numbers in [0, 100].
    dqScoreMap: {},
    // Whether the DQ map has already been fetched successfully (or at least attempted).
    dqLoaded: false,
    // ---- Phase-A additions (schema frozen: existing fields preserved, only additive below) ----
    // User bookmarks persisted in localStorage (cap 100): { id, name, nodeType, query, createdAt }.
    bookmarks: [],
    // Recently browsed root nodes persisted in localStorage (FIFO cap 10): { id, name, nodeType, visitedAt }.
    recentNodes: [],
    // Collapsed Domain/Owner tree keys for LgAssetTree. Object map (stable for serialization).
    treeCollapsedNodes: {},
    // Toolbar group indices (1..7) collapsed/responsively hidden at narrow breakpoints.
    toolbarGroupCollapsed: [],
    // Statusbar render stats reported by LineageV2 after each graph render.
    renderStats: { nodes: 0, edges: 0, ms: 0 },
    // WebSocket realtime connection flag consumed by the statusbar badge.
    wsConnected: false
  }),
  getters: {
    fieldLinks: (state) => {
      if (!state.selectedNode || !state.graphData) return []
      const nodeId = state.selectedNode.id
      return state.graphData.links
        .filter(l => (l.source === nodeId || l.target === nodeId) && l.fieldLinks?.length)
        .flatMap(l => l.fieldLinks || [])
    },
    /**
     * Returns the manual { x, y } override for a given node id, or null when unset.
     */
    layoutOverrideForId: (state) => (id) => state.layoutOverrides[id] || null,
    /**
     * Returns the normalized numeric data-quality score for a given node id, or null when absent.
     */
    dqScoreForId: (state) => (id) => state.dqScoreMap[id] != null ? Number(state.dqScoreMap[id]) : null
  },
  actions: {
    async loadGraph() {
      const tid = this.rootNodeId
      if (!tid) return
      this.graphLoading = true
      // Reset manual node positions whenever the graph structure is replaced.
      this.clearOverrides()
      this.selectedNode = null
      try {
        const data = await lineageApi.getGraph({
          nodeId: tid,
          direction: this.graphDirection,
          depth: this.graphDepth,
          level: this.graphLevel,
          nodeTypes: this.nodeTypeFilter || undefined
        })
        if (this.rootNodeId !== tid) return
        this.graphData = data
        this.isOverview = false
        this.buildNameMap(data)
        // Record the successfully loaded root node into the recent-browse stack (FIFO 10).
        this.addRecent({ id: this.rootNodeId, name: this.nodeNameMap[this.rootNodeId] || data.rootTableName || this.rootNodeId })
      } catch (e) {
        if (this.rootNodeId === tid) throw e
      } finally {
        if (this.rootNodeId === tid) this.graphLoading = false
      }
    },
    async loadOverview() {
      this.isOverview = true
      this.rootNodeId = null
      this.selectedNode = null
      this.graphLoading = true
      // Reset manual node positions whenever the graph structure is replaced.
      this.clearOverrides()
      try {
        const data = await lineageApi.getOverview()
        this.graphData = data
        this.buildNameMap(data)
      } finally {
        this.graphLoading = false
      }
    },
    buildNameMap(data) {
      this.nodeNameMap = {}
      for (const n of data.nodes || []) this.nodeNameMap[n.id] = n.name
    },
    selectNode(id) {
      if (this.rootNodeId === id) return
      // Drop stale overrides before reloading graph with new root.
      this.clearOverrides()
      this.rootNodeId = id
      if (id) {
        this.loadGraph()
      } else {
        this.graphData = null
        this.selectedNode = null
        this.graphLoading = false
      }
    },
    refocus(id) {
      // Drop stale overrides before reloading graph for the focused node.
      this.clearOverrides()
      this.rootNodeId = id
      this.isOverview = false
      this.selectedNode = null
      return this.loadGraph()
    },
    clearSelection() {
      this.selectedNode = null
    },

    /**
     * Toggle the visibility of a named layer ("entity" | "column" | "dq").
     * When enabling the dq layer and no scores have been loaded yet, fetches them
     * from the backend and normalizes the result into dqScoreMap.
     *
     * @param {'entity'|'column'|'dq'} key - Layer identifier.
     */
    toggleLayer(key) {
      this.layers[key] = !this.layers[key]
      if (key === 'dq' && this.layers.dq && !this.dqLoaded) {
        this.loadDqMap(false)
      }
    },

    /**
     * Record a manual layout position override for a node.
     *
     * @param {string} id - Node identifier.
     * @param {number} x  - X coordinate in canvas space.
     * @param {number} y  - Y coordinate in canvas space.
     */
    setOverride(id, x, y) {
      this.layoutOverrides[id] = { x, y }
    },

    /**
     * Remove all manual layout overrides, letting the algorithm recompute positions.
     */
    clearOverrides() {
      this.layoutOverrides = {}
    },

    /**
     * Toggle highlighting of edges whose field links mention the given column name.
     * If the matching edge set equals the currently highlighted set, highlighting is cleared.
     *
     * @param {string} columnName - Column name to match against fieldLinks.
     */
    toggleColumnHighlight(columnName) {
      const matchedIds = []
      for (const link of (this.graphData?.links || [])) {
        const hits = (link.fieldLinks || []).some(
          fl => fl.sourceColumn === columnName || fl.targetColumn === columnName
        )
        if (hits && link.id != null) matchedIds.push(String(link.id))
      }
      const current = [...this.highlightedFieldEdgeIds].map(String)
      const same =
        matchedIds.length === current.length &&
        matchedIds.slice().sort().join(',') === current.slice().sort().join(',')
      this.highlightedFieldEdgeIds = same ? [] : matchedIds
    },

    /**
     * Remove any active column-based edge highlighting.
     */
    clearColumnHighlight() {
      this.highlightedFieldEdgeIds = []
    },

    /**
     * Load (or force-reload) the data-quality score map into the store.
     * Tolerates both response shapes: an array [{nodeId, score}] or a map {[nodeId]: score}.
     * Network/parse errors are swallowed silently so UI toggles stay functional.
     *
     * @param {boolean} [force=false] - When true, refetch even if already loaded.
     */
    async loadDqMap(force = false) {
      if (!force && this.dqLoaded) return
      try {
        const result = await lineageApi.getDataQuality()
        const out = {}
        if (Array.isArray(result)) {
          for (const entry of result) {
            if (entry && entry.nodeId != null && entry.score != null) {
              out[String(entry.nodeId)] = entry.score
            }
          }
        } else if (result && typeof result === 'object') {
          for (const [key, value] of Object.entries(result)) {
            if (value != null) out[key] = value
          }
        }
        this.dqScoreMap = out
      } catch (_e) {
        // Ignore failures; dq layer just renders empty heat.
      } finally {
        this.dqLoaded = true
      }
    },

    // ---- Phase-A persistence actions (all localStorage access is best-effort) ----

    /**
     * Read persisted recent nodes from localStorage into recentNodes.
     * Silently swallows SSR / private-mode failures.
     */
    loadRecent() {
      try {
        const raw = localStorage.getItem('td_lineage_recent')
        this.recentNodes = raw ? JSON.parse(raw) : []
        if (!Array.isArray(this.recentNodes)) this.recentNodes = []
      } catch (_e) {
        this.recentNodes = []
      }
    },

    /**
     * Push a node onto the recent-browse stack (FIFO, cap 10) and persist it.
     * Keeps a single entry per id (bumping it to the front on re-visit).
     *
     * @param {{id: string, name?: string, nodeType?: string}} node - Recently viewed root node.
     */
    addRecent(node) {
      if (!node || node.id == null) return
      const item = {
        id: String(node.id),
        name: node.name || String(node.id),
        nodeType: node.nodeType || '',
        visitedAt: Date.now()
      }
      this.loadRecent()
      this.recentNodes = [item, ...this.recentNodes.filter(r => r.id !== item.id)].slice(0, 10)
      this.persistRecent()
    },

    /**
     * Write the current recentNodes array to localStorage (best-effort).
     */
    persistRecent() {
      try {
        localStorage.setItem('td_lineage_recent', JSON.stringify(this.recentNodes))
      } catch (_e) { /* ignore storage failures */ }
    },

    /**
     * Read persisted bookmarks from localStorage into bookmarks (cap 100).
     */
    loadBookmarks() {
      try {
        const raw = localStorage.getItem('td_lineage_bookmarks')
        this.bookmarks = raw ? JSON.parse(raw) : []
        if (!Array.isArray(this.bookmarks)) this.bookmarks = []
      } catch (_e) {
        this.bookmarks = []
      }
    },

    /**
     * Add (or refresh) a bookmark and persist it. Bookmark ids are unique;
     * re-adding an existing id just bumps its position. Cap 100 records.
     *
     * @param {{id: string, name?: string, nodeType?: string, query?: object}} bookmark - Bookmark descriptor.
     */
    addBookmark(bookmark) {
      if (!bookmark || bookmark.id == null) return
      const entry = {
        id: String(bookmark.id),
        name: bookmark.name || String(bookmark.id),
        nodeType: bookmark.nodeType || '',
        query: bookmark.query || null,
        createdAt: Date.now()
      }
      this.loadBookmarks()
      this.bookmarks = [
        entry,
        ...this.bookmarks.filter(b => b.id !== entry.id)
      ].slice(0, 100)
      this.persistBookmarks()
    },

    /**
     * Remove a bookmark by node id and persist the updated list.
     *
     * @param {string} id - Bookmark / node id to remove.
     */
    removeBookmark(id) {
      this.loadBookmarks()
      this.bookmarks = this.bookmarks.filter(b => b.id !== String(id))
      this.persistBookmarks()
    },

    /**
     * Write the current bookmarks array to localStorage (best-effort).
     */
    persistBookmarks() {
      try {
        localStorage.setItem('td_lineage_bookmarks', JSON.stringify(this.bookmarks))
      } catch (_e) { /* ignore storage failures */ }
    },

    /**
     * Report statusbar render stats (node/edge counts + render duration in ms).
     *
     * @param {{nodes: number, edges: number, ms: number}} stats - Render measurement.
     */
    setRenderStats(stats) {
      this.renderStats = {
        nodes: stats?.nodes ?? 0,
        edges: stats?.edges ?? 0,
        ms: stats?.ms ?? 0
      }
    },

    /**
     * Update the WebSocket realtime connection badge state.
     *
     * @param {boolean} connected - Whether the realtime socket is currently open.
     */
    setWsConnected(connected) {
      this.wsConnected = !!connected
    }
  }
})

// NOTE: layout mode change triggers store.clearOverrides() from Lineage.vue onMounted via a watch on store.layoutMode.
