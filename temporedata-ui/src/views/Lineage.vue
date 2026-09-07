<template>
  <div class="lineage-page">
    <!-- Toolbar -->
    <div class="lineage-toolbar">
      <h2 class="page-title">数据血缘分析</h2>
      <div class="toolbar-center">
        <el-select
          v-model="store.searchKeyword"
          filterable
          remote
          :remote-method="handleSearch"
          :loading="searching"
          placeholder="搜索表 / 字段 / 任务..."
          clearable
          size="default"
          style="width: 300px"
          @change="selectNode"
        >
          <el-option
            v-for="t in searchResults"
            :key="t.id"
            :label="t.name"
            :value="t.id"
          >
            <div class="search-option">
              <el-tag :type="nodeTypeTag(t.nodeType)" size="small">{{ t.nodeType }}</el-tag>
              <span>{{ t.name }}</span>
              <span class="search-degree">↓{{ t.inDegree }} ↑{{ t.outDegree }}</span>
            </div>
          </el-option>
        </el-select>
        <el-radio-group v-model="store.graphLevel" size="small">
          <el-radio-button value="table">表级</el-radio-button>
          <el-radio-button value="column">字段级</el-radio-button>
        </el-radio-group>
        <!-- Advanced filters (Task 11) -->
        <el-select v-model="filterDomain" size="small" clearable placeholder="Domain" style="width:110px">
          <el-option v-for="d in domainOptions" :key="d" :label="d" :value="d" />
        </el-select>
        <el-select v-model="filterOwner" size="small" clearable placeholder="Owner" style="width:100px">
          <el-option v-for="o in ownerOptions" :key="o" :label="o" :value="o" />
        </el-select>
        <el-select v-model="filterTag" size="small" clearable placeholder="Tag" style="width:100px">
          <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="filterColumn" size="small" filterable clearable placeholder="Column" style="width:110px" @change="colFilterChanged">
          <el-option v-for="c in columnOptions" :key="c" :label="c" :value="c" />
        </el-select>
        <el-radio-group v-model="filterMode" size="small">
          <el-radio-button value="dim">Dim</el-radio-button>
          <el-radio-button value="hide">Hide</el-radio-button>
        </el-radio-group>
        <el-divider direction="vertical" />
        <el-select v-model="store.graphDirection" size="small" style="width: 110px">
          <el-option label="双向" value="both" />
          <el-option label="仅上游" value="up" />
          <el-option label="仅下游" value="down" />
        </el-select>
        <el-select v-model="store.graphDepth" size="small" style="width: 90px">
          <el-option v-for="d in [1,2,3,4,5]" :key="d" :label="`深度 ${d}`" :value="d" />
        </el-select>
        <el-select v-model="store.nodeTypeFilter" size="small" clearable placeholder="节点类型" style="width: 120px">
          <el-option label="表" value="TABLE" />
          <el-option label="字段" value="COLUMN" />
          <el-option label="任务" value="TASK" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="store.loadGraph()" :loading="store.graphLoading" :disabled="!store.rootNodeId">
          分析血缘
        </el-button>
        <el-button :icon="Share" :type="store.isOverview ? 'primary' : ''" @click="loadOverview" :disabled="store.isOverview">
          血缘总览
        </el-button>
        <!-- Layers chip (Task 8) -->
        <span class="tb-label">图层</span>
        <el-checkbox-button
          :model-value="store.layers.entity"
          :label="'entity'"
          @change="store.toggleLayer('entity')"
        >实体</el-checkbox-button>
        <el-checkbox-button
          :model-value="store.layers.column"
          :label="'column'"
          :disabled="!store.layers.entity && graphNodes.length > 200"
          :title="(!store.layers.entity && graphNodes.length > 200) ? '字段级节点过多，请减少分析深度后切换' : '列级图层'"
          @change="store.toggleLayer('column')"
        >字段</el-checkbox-button>
        <el-checkbox-button
          :model-value="store.layers.dq"
          :label="'dq'"
          :title="store.layers.dq ? '显示数据质量徽章' : '隐藏数据质量徽章'"
          @change="store.toggleLayer('dq')"
        >质量</el-checkbox-button>
        <el-divider direction="vertical" />
        <el-radio-group v-model="store.layoutMode" size="small">
          <el-radio-button value="force">力导向</el-radio-button>
          <el-radio-button value="layer">分层</el-radio-button>
          <el-radio-button value="radial">径向</el-radio-button>
        </el-radio-group>
        <el-button :icon="DataAnalysis" @click="runGraphAnalysis" :disabled="!store.graphData">环检测</el-button>
        <el-tag v-if="wsConnected" size="small" type="success">实时连接</el-tag>
        <el-tag v-else size="small" type="info">离线</el-tag>
      </div>
      <div class="toolbar-actions">
        <el-divider direction="vertical" />
        <el-button :icon="Guide" @click="openPathDialog" :disabled="!store.graphData">路径分析</el-button>
        <el-button :icon="Odometer" @click="loadHeat">热度排行</el-button>
        <el-button :icon="Document" @click="openSqlDialog">SQL 解析</el-button>
        <el-button :icon="Link" :disabled="!canWrite" :aria-disabled="!canWrite" title="从资产库添加血缘（LINEAGE:WRITE）" @click="openAssetAddEdgeDialog">添加血缘</el-button>
        <el-dropdown @command="handleExportCommand" :disabled="!store.graphData">
          <el-button :icon="Download">导出<el-icon class="el-icon--right"><ArrowRight /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="png">导出 PNG</el-dropdown-item>
              <el-dropdown-item command="svg">导出 SVG</el-dropdown-item>
              <el-dropdown-item command="csv">导出 CSV</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button :icon="RefreshRight" @click="rebuildGraph">重建图数据</el-button>
      </div>
    </div>

    <div class="lineage-body">
      <!-- Read-mode graph canvas (G6) + main detail panel -->
      <div class="graph-container">
          <div v-if="!store.graphData && !store.graphLoading" class="empty-state">
            <el-icon class="empty-icon"><Share /></el-icon>
            <p>搜索并选择表 / 字段 / 任务，点击"分析血缘"查看图谱</p>
          </div>
          <div v-if="store.graphLoading" class="loading-state">
            <el-icon class="loading-icon is-loading"><Loading /></el-icon>
            <p>正在分析血缘关系...</p>
          </div>
          <div v-if="store.graphData" class="graph-stats">
            <span>根节点：<strong>{{ store.graphData.rootTableName }}</strong></span>
            <el-tag v-if="!store.isOverview" size="small" type="success">上游 {{ store.graphData.totalUpstream }}</el-tag>
            <el-tag v-if="!store.isOverview" size="small" type="warning">下游 {{ store.graphData.totalDownstream }}</el-tag>
            <el-tag size="small" type="info">最大深度 {{ store.graphData.maxDepth }}</el-tag>
            <el-tag size="small">节点 {{ store.graphData.nodes?.length || 0 }}</el-tag>
            <el-tag size="small">连线 {{ store.graphData.links?.length || 0 }}</el-tag>
            <el-tag v-if="store.graphData.nodes?.length > 50" size="small" type="danger" effect="light">
              大图已聚合（{{ store.graphData.nodes.length }} → {{ aggregatedRenderCount }}）
            </el-tag>
          </div>
          <LineageGraph
            v-if="store.graphData"
            :data="viewGraphData"
            :layout="store.layoutMode"
            :cycle-edge-keys="cycleEdgeKeys"
            :layers="store.layers"
            :layout-overrides="store.layoutOverrides"
            :dq-score-map="store.dqScoreMap"
            :highlighted-field-edge-ids="store.highlightedFieldEdgeIds"
            :filter-keep-ids="filterKeepIds"
            :filter-dim-enabled="filterDimEnabled"
            :center-node-id="centerNodeId"
            :can-write="canWrite"
            :screenshot-lock="screenshotLock"
            @node-click="onNodeClick"
            @node-dblclick="onNodeRefocus"
            @edge-edit="onEdgeEdit"
            @edge-delete="onEdgeDelete"
            @ctx-add-upstream="(p)=>openAddEdgeDialog({ presetDir:'up', presetNodeId:p.nodeId })"
            @ctx-add-downstream="(p)=>openAddEdgeDialog({ presetDir:'down', presetNodeId:p.nodeId })"
            @node-dragend="(ev)=>{store.setOverride(ev.nodeId, ev.x, ev.y); ElMessage.success('已暂存该节点位置（切布局或换根会失效）', 2000);}"
            @update:screenshot-lock="(v)=>{ screenshotLock.value = v; }"
            ref="g6Ref"
          />
        </div>

        <div v-if="store.selectedNode" class="detail-panel">
          <!-- §1 摘要：面包屑 + 核心标识 -->
          <div class="dp-breadcrumb">数据资产 / 血缘分析 / {{ store.selectedNode.name }}</div>
          <div class="dp-head">
            <span class="dp-chip" :style="{ background: nodeChip(store.selectedNode).bg, color: nodeChip(store.selectedNode).fg }">{{ nodeChip(store.selectedNode).abbr }}</span>
            <span class="dp-name">{{ store.selectedNode.name }}</span>
            <el-tag :type="nodeTypeTag(store.selectedNode.nodeType)" size="small" class="dp-type">{{ store.selectedNode.nodeType }}</el-tag>
            <span v-if="store.layers.dq" class="dp-dq" :class="dqLevel(store.selectedNode)" :title="'DQ 分数 ' + (store.dqScoreForId?.(store.selectedNode.id) ?? '?')">
              {{ store.dqScoreForId?.(store.selectedNode.id) ?? '?' }}
            </span>
            <el-button v-if="navClip" link size="small" :icon="DocumentCopy" @click="copyFqn" title="复制节点 FQN / ID" />
          </div>
          <div class="dp-meta">
            <span>数据源 {{ store.selectedNode.datasourceName || '–' }}</span>
            <span>层 {{ store.selectedNode.layer ?? '–' }}</span>
          </div>

          <!-- §2 详情：字段血缘（可搜索、可折叠） -->
          <div class="dp-section">
            <div class="dp-sec-title" @click="detailColsOpen = !detailColsOpen">
              <span>Columns</span>
              <span class="dp-sec-arrow">{{ detailColsOpen ? '▲' : '▼' }}</span>
            </div>
            <template v-if="detailColsOpen">
              <el-input v-model="detailColSearch" size="small" placeholder="Search Columns" clearable class="dp-search" />
              <div class="dp-col-list">
                <div
                  v-for="c in detailColsFiltered"
                  :key="c"
                  class="dp-col"
                  :title="'点击高亮画布上的字段血缘边：' + c"
                  @click="store.toggleColumnHighlight(c)"
                >
                  <span class="dp-col-name">{{ c }}</span>
                  <span class="dp-col-type">{{ colTypeOf(c) }}</span>
                </div>
                <div v-if="!detailColsFiltered.length" class="dp-empty">暂无字段血缘</div>
              </div>
            </template>
          </div>

          <!-- §3 关联：上游/下游嵌套卡片 -->
          <div class="dp-section">
            <div class="dp-sec-title"><span>关联节点</span></div>
            <div class="dp-neighbors">
              <div v-for="nb in neighbors" :key="nb.id" class="dp-node-card" @click="store.refocus(nb.id)">
                <span class="dp-nb-dir" :class="nb.dir">{{ nb.dir === 'up' ? '上游' : '下游' }}</span>
                <span class="dp-nb-name" :title="nb.name">{{ nb.name }}</span>
                <el-button
                  v-if="canWrite"
                  link
                  type="danger"
                  size="small"
                  :icon="Delete"
                  class="dp-nb-remove"
                  title="移除该关联"
                  @click.stop="(ev) => { ev.preventDefault(); removeNeighborLink(nb); }"
                />
              </div>
              <div v-if="!neighbors.length" class="dp-empty">无直接关联节点</div>
            </div>
            <el-button
              size="small"
              type="primary"
              plain
              class="dp-add-link"
              :icon="Plus"
              :disabled="!canWrite || !store.selectedNode"
              :aria-disabled="!canWrite || !store.selectedNode"
              :title="canWrite ? '新建与当前节点的血缘关联（上游或下游）' : '缺少 LINEAGE:WRITE 权限'"
              @click="openAddEdgeDialog({ presetDir: 'down' })"
            >＋ 添加关联节点</el-button>
          </div>

          <div class="dp-actions">
            <el-button size="small" @click="()=>{store.clearOverrides(); ElMessage.success('已清除位置覆盖，布局即将回归')}">清除位置覆盖</el-button>
            <el-button type="primary" size="small" @click="store.refocus(store.selectedNode.id)">以此节点为中心</el-button>
            <el-button size="small" @click="store.clearSelection()">关闭</el-button>
          </div>
        </div>
      </div>

    <!-- Path Analysis Dialog -->
    <el-dialog v-model="pathDialogVisible" title="路径分析" width="620px" destroy-on-close>
      <div class="path-dialog-body">
        <div class="path-selects">
          <div class="path-field">
            <span class="path-label">起点</span>
            <el-select v-model="pathSource" filterable remote :remote-method="(k) => searchFor(k, 'pathSource')"
                       :loading="searching" placeholder="搜索起点节点" style="width: 100%">
              <el-option v-for="t in pathSourceResults" :key="t.id" :label="`${t.name} (${t.nodeType})`" :value="t.id" />
            </el-select>
          </div>
          <div class="path-field">
            <span class="path-label">终点</span>
            <el-select v-model="pathTarget" filterable remote :remote-method="(k) => searchFor(k, 'pathTarget')"
                       :loading="searching" placeholder="搜索终点节点" style="width: 100%">
              <el-option v-for="t in pathTargetResults" :key="t.id" :label="`${t.name} (${t.nodeType})`" :value="t.id" />
            </el-select>
          </div>
        </div>
        <div class="path-dialog-actions">
          <el-button type="primary" :loading="pathLoading" @click="runPathAnalysis">查找最短路径</el-button>
        </div>
        <div v-if="pathResult" class="path-result">
          <template v-if="pathResult.found">
            <el-alert type="success" :closable="false" :title="`找到路径（${pathResult.path.length} 个节点）`" />
            <div class="path-chain">
              <template v-for="(id, i) in pathResult.path" :key="i">
                <span v-if="i > 0" class="chain-arrow">→</span>
                <el-tag :type="i === 0 ? 'primary' : i === pathResult.path.length - 1 ? 'danger' : ''" size="small">
                  {{ pathName(id) }}
                </el-tag>
              </template>
            </div>
            <div v-if="pathResult.hops.length" class="path-hops">
              <h4>路径详情</h4>
              <el-table :data="pathResult.hops" size="small" border>
                <el-table-column label="起点" min-width="130" show-overflow-tooltip>
                  <template #default="{ row }">{{ pathName(row.source) }}</template>
                </el-table-column>
                <el-table-column label="终点" min-width="130" show-overflow-tooltip>
                  <template #default="{ row }">{{ pathName(row.target) }}</template>
                </el-table-column>
                <el-table-column prop="edgeType" label="边类型" width="110" />
                <el-table-column prop="taskName" label="任务" min-width="140" show-overflow-tooltip />
              </el-table>
            </div>
          </template>
          <el-alert v-else type="warning" :closable="false" title="未找到连通路径（两者之间不存在血缘链路）" />
        </div>
      </div>
    </el-dialog>

    <!-- Heat Dialog -->
    <el-dialog v-model="heatDialogVisible" title="热度排行（出入度 TOP20）" width="560px" destroy-on-close>
      <el-table :data="heatData" size="small" border max-height="480" v-loading="heatLoading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="nodeType" label="类型" width="90">
          <template #default="{ row }"><el-tag :type="nodeTypeTag(row.nodeType)" size="small">{{ row.nodeType }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="name" label="节点" min-width="160" show-overflow-tooltip />
        <el-table-column prop="inDegree" label="入度" width="70" align="center" />
        <el-table-column prop="outDegree" label="出度" width="70" align="center" />
        <el-table-column prop="total" label="热度" width="80" align="center">
          <template #default="{ row }"><el-tag size="small" type="danger">{{ row.total }}</el-tag></template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- SQL Analysis Dialog -->
    <el-dialog v-model="sqlDialogVisible" title="SQL 血缘解析" width="680px" destroy-on-close>
      <div class="sql-dialog-body">
        <p class="sql-dialog-hint">输入 INSERT...SELECT / CREATE TABLE AS SELECT，解析表间血缘与字段映射</p>
        <el-input v-model="sqlText" type="textarea" :rows="6" placeholder="例如：INSERT INTO dws_sales_daily (sale_date, total_amount) SELECT sale_date, SUM(amount) FROM dwd_sales_detail GROUP BY sale_date" />
        <div class="sql-dialog-actions">
          <el-button type="primary" :loading="sqlAnalyzing" @click="runSqlAnalysis">解析血缘</el-button>
        </div>
        <div v-if="sqlResult" class="sql-result">
          <div class="sql-result-head">
            <el-tag type="primary">{{ sqlResult.sqlType }}</el-tag>
            <span class="sql-result-arrow">{{ (sqlResult.inputTables || []).join(', ') || '-' }} → {{ sqlResult.outputTable || '(查询)' }}</span>
          </div>
          <template v-if="sqlResult.fieldMappings && sqlResult.fieldMappings.length">
            <h4>字段级血缘</h4>
            <el-table :data="sqlResult.fieldMappings" size="small" border max-height="220">
              <el-table-column prop="sourceTable" label="来源表" width="150" show-overflow-tooltip />
              <el-table-column prop="sourceColumn" label="来源列" width="130" show-overflow-tooltip />
              <el-table-column prop="targetColumn" label="目标列" width="130" show-overflow-tooltip />
              <el-table-column prop="transformation" label="转换逻辑" min-width="160" show-overflow-tooltip />
            </el-table>
          </template>
        </div>
      </div>
    </el-dialog>

    <!-- Edge Edit Dialog -->
    <el-dialog v-model="editEdgeVisible" title="编辑血缘关系" width="520px" destroy-on-close>
      <el-form :model="editEdgeForm" label-width="88px" size="default">
        <el-form-item label="来源"><el-input :model-value="selectedEdgeNames?.source" disabled /></el-form-item>
        <el-form-item label="目标"><el-input :model-value="selectedEdgeNames?.target" disabled /></el-form-item>
        <el-form-item label="关系类型">
          <el-select v-model="editEdgeForm.relationType" placeholder="FLOWS_TO / DERIVED_FROM / PRODUCES / CONSUMES" style="width:100%">
            <el-option label="数据流转 FLOWS_TO" value="FLOWS_TO" />
            <el-option label="衍生 DERIVED_FROM" value="DERIVED_FROM" />
            <el-option label="产出 PRODUCES" value="PRODUCES" />
            <el-option label="消费 CONSUMES" value="CONSUMES" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联任务">
          <el-input v-model="editEdgeForm.taskName" placeholder="（可选）任务名或工作流节点名" />
        </el-form-item>
        <el-form-item label="SQL 片段">
          <el-input v-model="editEdgeForm.sql" type="textarea" :rows="3" placeholder="（可选）执行的 SQL 或伪代码" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="editEdgeForm.description" type="textarea" :rows="2" placeholder="（可选）业务说明 / 审计备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editEdgeVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingEdge" @click="submitPatchEdge">保存</el-button>
      </template>
    </el-dialog>

    <!-- Generic Add Edge Dialog -->
    <el-dialog v-model="addEdgeVisible" title="新建血缘关系" width="560px" destroy-on-close>
      <el-form :model="addEdgeForm" label-width="88px" size="default">
        <el-form-item label="方向">
          <el-radio-group v-model="addEdgeForm.direction" :disabled="!!addEdgeForm.presetDir">
            <el-radio-button value="up">上游（peer → 当前节点）</el-radio-button>
            <el-radio-button value="down">下游（当前节点 → peer）</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="当前节点">
          <el-input :model-value="store.selectedNode?.name || addEdgeForm.presetNodeName || '–'" disabled />
        </el-form-item>
        <el-form-item label="关联节点">
          <el-select
            v-model="addEdgeForm.peerId"
            filterable
            remote
            :remote-method="(k)=>searchFor(k,'addEdgePeer')"
            :loading="searching"
            placeholder="搜索关联节点"
            style="width:100%"
          >
            <el-option v-for="t in addEdgePeerResults" :key="t.id" :label="`${t.name} (${t.nodeType})`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系类型">
          <el-select v-model="addEdgeForm.relationType" style="width:100%">
            <el-option label="数据流转 FLOWS_TO" value="FLOWS_TO" />
            <el-option label="衍生 DERIVED_FROM" value="DERIVED_FROM" />
            <el-option label="产出 PRODUCES" value="PRODUCES" />
            <el-option label="消费 CONSUMES" value="CONSUMES" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联任务"><el-input v-model="addEdgeForm.taskName" /></el-form-item>
        <el-form-item label="SQL 片段"><el-input v-model="addEdgeForm.sql" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="addEdgeForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addEdgeVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingEdge" @click="submitSaveEdge">保存并刷新</el-button>
      </template>
    </el-dialog>

    <!-- Asset Add Edge Dialog -->
    <el-dialog v-model="assetEdgeVisible" title="从资产库添加血缘" width="600px" destroy-on-close>
      <p class="dialog-hint">在两个资产节点之间直接创建血缘边（需要 LINEAGE:WRITE 权限）。</p>
      <el-form :model="assetEdgeForm" label-width="88px" size="default">
        <el-form-item label="源节点">
          <el-select v-model="assetEdgeForm.sourceId" filterable remote :remote-method="(k)=>searchFor(k,'assetSrc')" :loading="searching" placeholder="搜索源节点" style="width:100%">
            <el-option v-for="t in assetSrcResults" :key="t.id" :label="`${t.name} (${t.nodeType})`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标节点">
          <el-select v-model="assetEdgeForm.targetId" filterable remote :remote-method="(k)=>searchFor(k,'assetTgt')" :loading="searching" placeholder="搜索目标节点" style="width:100%">
            <el-option v-for="t in assetTgtResults" :key="t.id" :label="`${t.name} (${t.nodeType})`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系类型">
          <el-select v-model="assetEdgeForm.relationType" style="width:100%">
            <el-option label="数据流转 FLOWS_TO" value="FLOWS_TO" />
            <el-option label="衍生 DERIVED_FROM" value="DERIVED_FROM" />
            <el-option label="产出 PRODUCES" value="PRODUCES" />
            <el-option label="消费 CONSUMES" value="CONSUMES" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联任务"><el-input v-model="assetEdgeForm.taskName" /></el-form-item>
        <el-form-item label="SQL 片段"><el-input v-model="assetEdgeForm.sql" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="assetEdgeForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assetEdgeVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingEdge" @click="submitAssetEdge">保存并定位到源</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Share, Loading, ArrowRight, DataAnalysis, Guide, Odometer, Document, Download, RefreshRight, DocumentCopy, Plus, Delete, Link } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { useLineageStore } from '@/stores/lineage'
import { lineageApi } from '@/api/modules/lineage'
import LineageGraph from '@/components/lineage/LineageGraph.vue'
import { resolveSource } from '@/components/lineage/vueflow/sourceLogoRegistry'
import { hasPerm } from '@/lib/permission'

const store = useLineageStore()
const route = useRoute()
// Extract more reactive slices from Pinia store (layers/dq/layout overrides/highlighted edges)
const { graphData, graphLoading, selectedNode, layoutMode, isOverview, nodeNameMap, fieldLinks, layers, layoutOverrides, highlightedFieldEdgeIds, dqScoreMap, dqLoaded } = storeToRefs(store)

// ---- Permission & screenshot lock ----
const screenshotLock = ref(false)
const canWrite = computed(() => hasPerm('LINEAGE:WRITE'))

// ---- Advanced filters (Task 11) ----
const filterDomain = ref(null)
const filterOwner = ref(null)
const filterTag = ref(null)
const filterColumn = ref(null)
const filterMode = ref('dim') // 'dim' | 'hide'

const graphNodes = computed(() => graphData.value?.nodes || [])

function uniqueValues(key) {
  const s = new Set()
  for (const n of graphNodes.value) {
    const v = n.data?.[key] ?? n.attributes?.[key] ?? n[key]
    if (v != null && String(v).trim()) s.add(v)
  }
  return [...s].sort()
}
function flatMap(fn, arr) {
  const out = []
  for (const e of arr) out.push(...(fn(e) || []))
  return out
}
function uniqueColumnNames() {
  const s = new Set()
  const links = graphData.value?.links || []
  for (const l of links) {
    for (const fl of (l.fieldLinks || [])) {
      if (fl.sourceColumn) s.add(fl.sourceColumn)
      if (fl.targetColumn) s.add(fl.targetColumn)
    }
  }
  return [...s].sort()
}

const domainOptions = computed(() => uniqueValues('domain'))
const ownerOptions = computed(() => uniqueValues('owner'))
const tagOptions = computed(() => Array.from(new Set(flatMap(n => n.data?.tags || n.attributes?.tags || [], graphNodes.value))))
const columnOptions = computed(() => uniqueColumnNames())

// Center node: use store.rootNodeId if set; otherwise fall back to category==='root' node.
const centerNodeId = computed(() => store.rootNodeId || (graphNodes.value.find(n => n.category === 'root')?.id))

// Clipboard availability guard for FQN copy button
const navClip = computed(() => typeof navigator !== 'undefined' && !!navigator.clipboard)

// ---- Filtered graph data (Task 11) ----
// In 'hide' mode with at least one active filter, compute AND-filtered nodes + BFS ±1 hop preserved neighbors.
// In 'dim' mode the current component defers visual dimming to a future LineageGraph iteration; pass raw graphData.
const viewGraphData = computed(() => {
  const raw = graphData.value
  if (!raw) return raw
  const hasFilter = filterDomain.value != null || filterOwner.value != null || filterTag.value != null || filterColumn.value != null
  // TODO: implement dim-mode visual dim state inside LineageGraph; currently hide mode is implemented here by pre-filtering data.
  if (filterMode.value !== 'hide' || !hasFilter) return raw

  const nodes = raw.nodes || []
  const links = raw.links || []
  const columnFilter = filterColumn.value

  // AND condition for a single node against domain/owner/tag filters; also column filter requires incident fieldLinks matching.
  function nodePassesFilter(n) {
    const nd = n.data ?? n.attributes ?? {}
    if (filterDomain.value != null) {
      const dom = nd.domain ?? n.domain
      if (dom !== filterDomain.value) return false
    }
    if (filterOwner.value != null) {
      const own = nd.owner ?? n.owner
      if (own !== filterOwner.value) return false
    }
    if (filterTag.value != null) {
      const tags = nd.tags ?? n.tags ?? []
      if (!tags.includes(filterTag.value)) return false
    }
    return true
  }
  // Collect node ids that pass the node-level AND filters
  const passIds = new Set()
  for (const n of nodes) {
    if (nodePassesFilter(n)) passIds.add(n.id)
  }
  // If column filter is active, also include nodes participating in fieldLinks matching the column name.
  if (columnFilter) {
    for (const l of links) {
      const fieldLinksArr = l.fieldLinks || []
      const match = fieldLinksArr.some(fl => fl.sourceColumn === columnFilter || fl.targetColumn === columnFilter)
      if (match) {
        passIds.add(l.source)
        passIds.add(l.target)
      }
    }
  }

  // BFS ±1 hop preservation
  const keepIds = new Set(passIds)
  for (const l of links) {
    if (passIds.has(l.source)) keepIds.add(l.target)
    if (passIds.has(l.target)) keepIds.add(l.source)
  }

  const keepNodes = nodes.filter(n => keepIds.has(n.id))
  const keepLinks = links.filter(l => keepIds.has(l.source) && keepIds.has(l.target))
  return { ...raw, nodes: keepNodes, links: keepLinks }
})

/**
 * IDs of nodes that survive the AND filter criteria (domain/owner/tag/column), plus their 1-hop bidirectional path neighbors.
 * Used in "dim" mode to apply filter-dim state incrementally on the graph without re-rendering the full data tree.
 */
const filterKeepIds = computed(() => {
  const raw = store.graphData.value
  if (!raw) return []
  const has = filterDomain.value != null || filterOwner.value != null || filterTag.value != null || filterColumn.value != null
  if (!has) return [] // No filters → nothing to dim.

  const nodes = raw.nodes || []
  const links = raw.links || []
  const colFilter = filterColumn.value

  // 1. Base passes filter
  const base = new Set()
  for (const n of nodes) {
    const d = n.data || n.attributes || n
    const tags = d.tags || d.tagList || []
    if (filterDomain.value != null && (d.domain ?? d.Domain) !== filterDomain.value) continue
    if (filterOwner.value != null && (d.owner ?? d.Owner) !== filterOwner.value) continue
    if (filterTag.value != null && !tags.includes(filterTag.value)) continue
    base.add(n.id)
  }

  // 2. If column filter active, include nodes participating in matching field links
  if (colFilter) {
    for (const l of links) {
      const hit = (l.fieldLinks || []).some(fl => fl.sourceColumn === colFilter || fl.targetColumn === colFilter)
      if (hit) { base.add(l.source); base.add(l.target) }
    }
  }
  if (!base.size) return []

  // 3. BFS ±1 hop neighbor preservation (same logic as viewGraphData hide mode)
  const keep = new Set(base)
  for (const l of links) {
    if (base.has(l.source) || base.has(l.target)) { keep.add(l.source); keep.add(l.target) }
  }
  return [...keep]
})

const filterDimEnabled = computed(() => filterMode.value === 'dim' && filterKeepIds.value.length > 0)

// ---- Aggregated node count (kept from original) ----
const aggregatedRenderCount = computed(() => {
  const nodes = store.graphData?.nodes || []
  if (nodes.length <= 50) return nodes.length
  // Aggregation heuristic matching LineageGraph: count L0 + up to 3 per other layer.
  const byLayer = {}
  for (const n of nodes) {
    const layer = n.layer ?? 0
    ;(byLayer[layer] = byLayer[layer] || []).push(n)
  }
  let count = 0
  for (const [layer, list] of Object.entries(byLayer)) {
    if (Number(layer) === 0 || list.length <= 3) count += list.length
    else count += 1
  }
  return count
})

// ---- Local state ----
const searching = ref(false)
const searchResults = ref([])
const g6Ref = ref(null)
const cycleEdgeKeys = ref([])

function nodeChip(n) {
  if (!n) return { abbr: '?', bg: '#e0e0e0', fg: '#546e7a' }
  const d = n.data || {}
  return resolveSource(d.engine || d.datasourceName || d.attributes?.datasourceName || d.attributes?.engine || d.database || '')
}

// ---- Right three-stage detail panel (read mode) ----
const detailColsOpen = ref(true)
const detailColSearch = ref('')
// Aggregate column names the selected node participates in via its field-level lineage.
const detailColsFiltered = computed(() => {
  const cols = []
  for (const fl of fieldLinks.value) {
    if (fl.sourceColumn) cols.push(fl.sourceColumn)
    if (fl.targetColumn) cols.push(fl.targetColumn)
  }
  const uniq = [...new Set(cols)]
  const kw = detailColSearch.value.trim().toLowerCase()
  return kw ? uniq.filter((c) => c.toLowerCase().includes(kw)) : uniq
})
// Direct neighbors (up/down) of the selected node as nested cards.
const neighbors = computed(() => {
  const id = store.selectedNode?.id
  const g = store.graphData
  if (!id || !g) return []
  const map = {}
  const order = {}
  let rank = 0
  for (const l of g.links || []) {
    if (l.source === id) { map[l.target] = 'down'; order[l.target] = rank++ }
    else if (l.target === id) { map[l.source] = 'up'; order[l.source] = rank++ }
  }
  return Object.keys(map).map((nid) => ({
    id: nid,
    name: nodeNameMap.value[nid] || nid,
    dir: map[nid],
    _ord: order[nid]
  })).sort((a, b) => a._ord - b._ord)
})

// Path analysis
const pathDialogVisible = ref(false)
const pathSource = ref('')
const pathTarget = ref('')
const pathSourceResults = ref([])
const pathTargetResults = ref([])
const pathLoading = ref(false)
const pathResult = ref(null)

// Heat
const heatDialogVisible = ref(false)
const heatLoading = ref(false)
const heatData = ref([])

// SQL analysis
const sqlDialogVisible = ref(false)
const sqlText = ref('')
const sqlAnalyzing = ref(false)
const sqlResult = ref(null)

// WebSocket
const wsConnected = ref(false)
let ws = null

// ---- Edge edit dialog state (Task 9a) ----
const editEdgeVisible = ref(false)
const selectedEdgeData = ref(null)
const selectedEdgeNames = computed(() => ({
  source: nodeNameMap.value[selectedEdgeData.value?.source] || selectedEdgeData.value?.source || '–',
  target: nodeNameMap.value[selectedEdgeData.value?.target] || selectedEdgeData.value?.target || '–',
}))
const editEdgeForm = ref({ relationType: '', taskName: '', sql: '', description: '' })

// ---- Generic add-edge dialog (from detail panel / ctx menu) ----
const addEdgeVisible = ref(false)
const addEdgePeerResults = ref([])
const addEdgeForm = ref({ direction: 'down', peerId: '', relationType: 'FLOWS_TO', taskName: '', sql: '', description: '', presetNodeId: null, presetNodeName: '' })

// ---- Asset add-edge dialog (Task 7) ----
const assetEdgeVisible = ref(false)
const assetSrcResults = ref([])
const assetTgtResults = ref([])
const assetEdgeForm = ref({ sourceId: '', targetId: '', relationType: 'FLOWS_TO', taskName: '', sql: '', description: '' })

const submittingEdge = ref(false)

// ---- Watchers ----
watch(() => [store.graphLevel, store.graphDirection, store.graphDepth, store.nodeTypeFilter], () => {
  if (store.rootNodeId && !store.graphLoading) store.loadGraph()
})

// Drop manual position overrides whenever user switches layout algorithm.
watch(() => store.layoutMode, () => { store.clearOverrides(); store.clearColumnHighlight() })

// ---- Helpers ----
function nodeTypeTag(t) {
  return t === 'TABLE' ? 'primary' : t === 'COLUMN' ? 'success' : t === 'TASK' ? 'warning' : 'info'
}
function nodeTypeLabel(t) {
  return t === 'TABLE' ? '数据表' : t === 'COLUMN' ? '字段' : t === 'TASK' ? '任务' : t === 'REPORT' ? '报表' : t || '节点'
}
function categoryTag(c) {
  return c === 'root' ? '' : c === 'upstream' ? 'success' : c === 'downstream' ? 'warning' : 'info'
}
function categoryLabel(c) {
  return c === 'root' ? '根节点' : c === 'upstream' ? '上游' : c === 'downstream' ? '下游' : '外部'
}
function pathName(id) {
  return nodeNameMap.value[id] || id
}

// DQ score -> level class for detail panel badge
function dqLevel(n) {
  const s = store.dqScoreForId?.(n?.id)
  if (s == null) return 'dq-none'
  return s >= 80 ? 'dq-high' : (s >= 60 ? 'dq-mid' : 'dq-low')
}

// Lookup column type by name from the selected node's column metadata
function colTypeOf(colName) {
  const cols = store.selectedNode?.data?.columns || store.selectedNode?.attributes?.columns || []
  const hit = cols.find(x => x && (x.name === colName || x.columnName === colName))
  if (hit) return hit.type || hit.dataType || hit.columnType || '-'
  return '-'
}

// CSV export helper: quote a single cell value if it contains comma/quote/newline
function csvQuote(val) {
  if (val == null) return ''
  const str = String(val)
  if (/[",\n\r]/.test(str)) return '"' + str.replace(/"/g, '""') + '"'
  return str
}
// Build a CSV string from an array of row objects
function toCsv(rows) {
  if (!rows || !rows.length) return ''
  const headers = Object.keys(rows[0])
  const head = headers.map(csvQuote).join(',')
  const body = rows.map(r => headers.map(h => csvQuote(r[h])).join(',')).join('\n')
  return head + '\n' + body
}
// Trigger a browser download of a Blob with the given filename
function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

// ---- Search ----
async function handleSearch(kw) {
  if (!kw || kw.length < 1) { searchResults.value = []; return }
  searching.value = true
  try {
    searchResults.value = await lineageApi.search(kw)
  } catch (e) {
    searchResults.value = []
  } finally { searching.value = false }
}

async function searchFor(kw, target) {
  if (!kw || kw.length < 1) {
    if (target === 'pathSource') pathSourceResults.value = []
    else if (target === 'pathTarget') pathTargetResults.value = []
    else if (target === 'addEdgePeer') addEdgePeerResults.value = []
    else if (target === 'assetSrc') assetSrcResults.value = []
    else if (target === 'assetTgt') assetTgtResults.value = []
    return
  }
  searching.value = true
  try {
    const res = await lineageApi.search(kw)
    if (target === 'pathSource') pathSourceResults.value = res
    else if (target === 'pathTarget') pathTargetResults.value = res
    else if (target === 'addEdgePeer') addEdgePeerResults.value = res
    else if (target === 'assetSrc') assetSrcResults.value = res
    else if (target === 'assetTgt') assetTgtResults.value = res
  } catch { /* ignore */ } finally { searching.value = false }
}

function selectNode(id) {
  store.selectNode(id)
}

// ---- Graph actions ----
async function loadOverview() {
  await store.loadOverview()
}

function onNodeClick(node) {
  store.selectedNode = node
}

function onNodeRefocus(node) {
  store.refocus(node.id)
  ElMessage.success(`已聚焦节点：${node.name}`)
}

// ---- Export (Task 12) ----
async function doExportPng() {
  const dataUrl = await g6Ref.value?.exportPng()
  if (!dataUrl) {
    ElMessage.warning('导出失败')
    return
  }
  const ts = new Date().toISOString().replace(/[:.]/g, '-')
  const a = document.createElement('a')
  a.href = dataUrl
  a.download = `lineage_${store.graphData?.rootTableName || 'graph'}_${ts}.png`
  a.click()
}
async function doExportSvg() {
  const instance = g6Ref.value?.getInstance?.()
  const dataUrl = instance?.toDataURL ? instance.toDataURL('image/svg+xml') : null
  if (!dataUrl) {
    ElMessage.warning('当前图实例不支持 SVG 导出，请改用 PNG')
    return
  }
  const ts = new Date().toISOString().replace(/[:.]/g, '-')
  const a = document.createElement('a')
  a.href = dataUrl
  a.download = `lineage_${store.graphData?.rootTableName || 'graph'}_${ts}.svg`
  a.click()
}
async function doExportCsv() {
  const data = viewGraphData.value || store.graphData.value
  if (!data) { ElMessage.warning('没有可导出的图数据'); return }
  const ts = new Date().toISOString().replace(/[:.]/g, '-')
  const base = store.graphData?.rootTableName || 'graph'
  const nodesCsv = toCsv((data.nodes || []).map(n => {
    const d = n.data || n.attributes || {}
    return {
      id: n.id,
      name: n.name,
      nodeType: n.nodeType,
      category: n.category || '',
      layer: n.layer ?? '',
      datasourceName: n.datasourceName || d.datasourceName || '',
      domain: d.domain || n.domain || '',
      owner: d.owner || n.owner || '',
      tags: (d.tags || n.tags || []).join('|'),
    }
  }))
  const linksCsv = toCsv((data.links || []).map(l => ({
    source: l.source,
    target: l.target,
    sourceName: nodeNameMap.value[l.source] || l.source,
    targetName: nodeNameMap.value[l.target] || l.target,
    edgeType: l.edgeType || l.relationType || 'FLOWS_TO',
    taskName: l.taskName || '',
    fieldCount: (l.fieldLinks || []).length,
  })))
  if (nodesCsv) downloadBlob(new Blob([nodesCsv], { type: 'text/csv;charset=utf-8;' }), `lineage_${base}_nodes_${ts}.csv`)
  if (linksCsv) downloadBlob(new Blob([linksCsv], { type: 'text/csv;charset=utf-8;' }), `lineage_${base}_links_${ts}.csv`)
  ElMessage.success('CSV 导出完成（nodes + links 两个文件）')
}
function handleExportCommand(cmd) {
  if (cmd === 'png') return doExportPng()
  if (cmd === 'svg') return doExportSvg()
  if (cmd === 'csv') return doExportCsv()
}

async function runGraphAnalysis() {
  if (!store.graphData) return
  try {
    const res = await lineageApi.findCycles()
    // Highlight cycle edges on the canvas (red).
    const keys = new Set()
    for (const cycle of res.cycles || []) {
      for (let i = 0; i < cycle.length - 1; i++) {
        const srcName = cycle[i]
        const tgtName = cycle[i + 1]
        // Resolve names back to ids via name map.
        const srcId = Object.keys(nodeNameMap.value).find(k => nodeNameMap.value[k] === srcName)
        const tgtId = Object.keys(nodeNameMap.value).find(k => nodeNameMap.value[k] === tgtName)
        if (srcId && tgtId) keys.add(`${srcId}|${tgtId}`)
      }
    }
    cycleEdgeKeys.value = [...keys]
    ElMessageBox.alert(
      res.summary + (res.cycles.length ? '\n环：' + res.cycles.map(c => c.join(' → ')).join('\n') : ''),
      '环检测结果',
      { confirmButtonText: '知道了', type: res.hasCycle ? 'warning' : 'success' }
    )
    if (!res.hasCycle) cycleEdgeKeys.value = []
  } catch (e) {
    ElMessage.error('环检测失败: ' + (e.message || '未知错误'))
  }
}

// ---- Edge handlers (edit / delete) ----
function onEdgeEdit(edgeData) {
  if (!canWrite.value) { ElMessage.warning('缺少 LINEAGE:WRITE 权限'); return }
  selectedEdgeData.value = edgeData
  // Pre-fill form from existing edge metadata (if any)
  const rel = edgeData?.relationType || edgeData?.edgeType || ''
  editEdgeForm.value = {
    relationType: rel,
    taskName: edgeData?.taskName || '',
    sql: edgeData?.sql || '',
    description: edgeData?.description || '',
  }
  editEdgeVisible.value = true
}
async function onEdgeDelete(edgeData) {
  if (!canWrite.value) { ElMessage.warning('缺少 LINEAGE:WRITE 权限'); return }
  if (!edgeData?.source || !edgeData?.target) { ElMessage.warning('无效的边数据'); return }
  try {
    await ElMessageBox.confirm(
      `确认删除 ${selectedEdgeNames.value.source} → ${selectedEdgeNames.value.target} 之间的血缘关系？`,
      '删除血缘边',
      { type: 'warning' }
    )
  } catch { return }
  try {
    await lineageApi.deleteEdge(edgeData.source, edgeData.target)
    ElMessage.success('已删除血缘边')
    await store.loadGraph()
  } catch (e) { ElMessage.error('删除失败：' + (e.message || '未知错误')) }
}
async function submitPatchEdge() {
  if (!selectedEdgeData.value?.source || !selectedEdgeData.value?.target) {
    ElMessage.warning('缺少边端点信息')
    return
  }
  const { source, target } = selectedEdgeData.value
  const { relationType, taskName, sql, description } = editEdgeForm.value
  submittingEdge.value = true
  try {
    await lineageApi.patchEdge(source, target, { relationType, taskName, sql, description })
    ElMessage.success('已保存血缘关系')
    editEdgeVisible.value = false
    await store.loadGraph()
  } catch (e) { ElMessage.error('保存失败：' + (e.message || '未知错误')) }
  finally { submittingEdge.value = false }
}

// ---- Generic add-edge dialog helpers ----
function openAddEdgeDialog({ presetDir, presetNodeId }) {
  if (!canWrite.value) { ElMessage.warning('缺少 LINEAGE:WRITE 权限'); return }
  // If presetNodeId is provided without a currently selected node, select it first.
  if (presetNodeId && (!store.selectedNode || store.selectedNode.id !== presetNodeId)) {
    store.selectNode(presetNodeId)
  }
  nextTick(() => {
    addEdgeForm.value = {
      direction: presetDir || 'down',
      peerId: '',
      relationType: 'FLOWS_TO',
      taskName: '',
      sql: '',
      description: '',
      presetNodeId,
      presetNodeName: nodeNameMap.value[presetNodeId] || presetNodeId,
    }
    addEdgePeerResults.value = []
    addEdgeVisible.value = true
  })
}
async function submitSaveEdge() {
  const cur = store.selectedNode?.id || addEdgeForm.value.presetNodeId
  if (!cur || !addEdgeForm.value.peerId) { ElMessage.warning('请选择关联节点'); return }
  const fromId = addEdgeForm.value.direction === 'down' ? cur : addEdgeForm.value.peerId
  const toId   = addEdgeForm.value.direction === 'down' ? addEdgeForm.value.peerId : cur
  const { relationType, taskName, sql, description } = addEdgeForm.value
  submittingEdge.value = true
  try {
    await lineageApi.saveEdge(fromId, toId, { relationType, taskName, sql, description })
    ElMessage.success('已创建血缘关系')
    addEdgeVisible.value = false
    // Refocus on the newly connected peer so the user sees both sides.
    await store.refocus(addEdgeForm.value.peerId)
  } catch (e) { ElMessage.error('保存失败：' + (e.message || '未知错误')) }
  finally { submittingEdge.value = false }
}

// ---- Asset add-edge dialog helpers (Task 7) ----
function openAssetAddEdgeDialog() {
  if (!canWrite.value) { ElMessage.warning('缺少 LINEAGE:WRITE 权限'); return }
  assetEdgeForm.value = { sourceId: '', targetId: '', relationType: 'FLOWS_TO', taskName: '', sql: '', description: '' }
  assetSrcResults.value = []
  assetTgtResults.value = []
  assetEdgeVisible.value = true
}
async function submitAssetEdge() {
  const { sourceId, targetId, relationType, taskName, sql, description } = assetEdgeForm.value
  if (!sourceId || !targetId) { ElMessage.warning('请选择源和目标节点'); return }
  submittingEdge.value = true
  try {
    await lineageApi.saveEdge(sourceId, targetId, { relationType, taskName, sql, description })
    ElMessage.success('已添加血缘关系')
    assetEdgeVisible.value = false
    // Refocus to source so user can visually verify the new edge from there.
    await store.refocus(sourceId)
  } catch (e) { ElMessage.error('保存失败：' + (e.message || '未知错误')) }
  finally { submittingEdge.value = false }
}

// ---- Detail panel helpers ----
async function copyFqn() {
  try {
    await navigator.clipboard.writeText(store.selectedNode.id)
    ElMessage.success('已复制节点 ID')
  } catch { ElMessage.warning('复制失败') }
}
async function removeNeighborLink(nb) {
  if (!canWrite.value) { ElMessage.warning('缺少 LINEAGE:WRITE 权限'); return }
  const curId = store.selectedNode?.id
  if (!curId) return
  const fromId = nb.dir === 'down' ? curId : nb.id
  const toId   = nb.dir === 'down' ? nb.id : curId
  try {
    await ElMessageBox.confirm(`确认移除与「${nb.name}」的血缘关联？此操作不可撤销。`, '删除关联', { type: 'warning' })
  } catch { return }
  try {
    await lineageApi.deleteEdge(fromId, toId)
    ElMessage.success('已移除关联')
    await store.loadGraph()
  } catch (e) { ElMessage.error('移除失败：' + (e.message || '未知错误')) }
}

// Column filter change handler for toolbar column select
function colFilterChanged(val) {
  if (val) store.toggleColumnHighlight(val)
  else store.clearColumnHighlight()
}

// ---- Path analysis ----
function openPathDialog() {
  pathSource.value = store.rootNodeId || ''
  pathTarget.value = ''
  pathResult.value = null
  pathSourceResults.value = []
  pathTargetResults.value = []
  pathDialogVisible.value = true
}

async function runPathAnalysis() {
  if (!pathSource.value || !pathTarget.value) {
    ElMessage.warning('请选择起点和终点')
    return
  }
  pathLoading.value = true
  try {
    pathResult.value = await lineageApi.findPath({ sourceId: pathSource.value, targetId: pathTarget.value })
  } catch (e) {
    ElMessage.error('路径分析失败: ' + (e.message || '未知错误'))
  } finally { pathLoading.value = false }
}

// ---- Heat ----
async function loadHeat() {
  heatDialogVisible.value = true
  heatLoading.value = true
  try {
    heatData.value = await lineageApi.getHeat()
  } catch (e) {
    ElMessage.error('获取热度失败: ' + (e.message || '未知错误'))
  } finally { heatLoading.value = false }
}

// ---- Rebuild ----
async function rebuildGraph() {
  try {
    await ElMessageBox.confirm('将根据元数据、同步任务、采集任务、质量规则、工作流 SQL 全量重建血缘图数据（约数十秒）。确认继续？', '重建图数据', { type: 'warning' })
  } catch { return }
  store.graphLoading = true
  try {
    const res = await lineageApi.rebuild()
    ElMessage.success(`重建完成：${res.totalNodes} 节点 / ${res.totalEdges} 边`)
    if (store.rootNodeId) await store.loadGraph()
    else await store.loadOverview()
  } catch (e) {
    ElMessage.error('重建失败: ' + (e.message || '未知错误'))
  } finally { store.graphLoading = false }
}

// ---- SQL analysis ----
function openSqlDialog() {
  sqlText.value = ''
  sqlResult.value = null
  sqlDialogVisible.value = true
}

async function runSqlAnalysis() {
  if (!sqlText.value || !sqlText.value.trim()) {
    ElMessage.warning('请输入 SQL')
    return
  }
  sqlAnalyzing.value = true
  try {
    const p = await lineageApi.parseSql(sqlText.value)
    sqlResult.value = {
      sqlType: p.sqlType,
      inputTables: p.sources || [],
      outputTable: p.targetTable,
      fieldMappings: (p.columnLineage || []).map(cl => ({
        sourceTable: cl.sourceTable,
        sourceColumn: cl.sourceColumn,
        targetColumn: cl.targetColumn,
        transformation: cl.transformation
      }))
    }
    if (!(sqlResult.value.inputTables.length || sqlResult.value.fieldMappings.length || sqlResult.value.outputTable)) {
      ElMessage.info('未解析到血缘关系')
    }
  } catch (e) {
    ElMessage.error('SQL 解析失败: ' + (e.message || '未知错误'))
  } finally { sqlAnalyzing.value = false }
}

// ---- WebSocket realtime ----
function connectWs() {
  try {
    const token = localStorage.getItem('td_token')
    if (!token) return
    const base = import.meta.env.DEV ? 'ws://localhost:8080' : `ws://${location.host}`
    ws = new WebSocket(`${base}/ws/lineage?token=${token}`)
    ws.onopen = () => { wsConnected.value = true }
    ws.onclose = () => {
      wsConnected.value = false
      setTimeout(connectWs, 5000)
    }
    ws.onerror = () => { wsConnected.value = false }
    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        if (msg.type === 'lineage.updated') {
          ElMessage.success(`血缘数据已更新（${msg.source || '采集'}）`)
          cycleEdgeKeys.value = []
          if (store.isOverview) store.loadOverview()
          else if (store.rootNodeId) store.loadGraph()
        }
      } catch (e) { /* ignore */ }
    }
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  const nodeQuery = route.query.node
  if (nodeQuery) {
    store.rootNodeId = String(nodeQuery)
    store.isOverview = false
    store.loadGraph().catch(() => {
      ElMessage.error('定位节点失败，已切换至总览')
      store.loadOverview()
    })
  } else {
    store.loadOverview()
  }
  // Preload DQ score map if DQ layer was already on when entering the page.
  if (store.layers.dq && !store.dqLoaded) store.loadDqMap(false)
  connectWs()
})

onBeforeUnmount(() => {
  ws?.close()
})
</script>

<style scoped>
.lineage-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 12px;
}
.lineage-toolbar {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}
.search-option {
  display: flex;
  align-items: center;
  gap: 8px;
}
.search-degree {
  color: #9ca3af;
  font-size: 12px;
  margin-left: auto;
}

.lineage-body {
  flex: 1;
  display: flex;
  gap: 12px;
  min-height: 0;
}

.graph-container {
  flex: 1;
  background: #f5f7fa;
  border-radius: 8px;
  position: relative;
  overflow: hidden;
  min-height: 0;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  white-space: nowrap;
}
.empty-state, .loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #6b7280;
}
.toolbar-center {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  flex-wrap: wrap;
}
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.search-option {
  display: flex;
  align-items: center;
  gap: 8px;
}
.search-degree {
  color: #9ca3af;
  font-size: 12px;
  margin-left: auto;
}

.lineage-body {
  flex: 1;
  display: flex;
  gap: 12px;
  min-height: 0;
}

.graph-container {
  flex: 1;
  background: #f5f7fa;
  border-radius: 8px;
  position: relative;
  overflow: hidden;
  min-height: 0;
}
.empty-state, .loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #6b7280;
}
.empty-icon, .loading-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.graph-stats {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: #37474f;
  font-size: 13px;
  z-index: 10;
  pointer-events: none;
}

.detail-panel {
  width: 320px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  padding: 14px 16px;
  overflow-y: auto;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.dp-breadcrumb {
  font-size: 11px;
  color: #90a4ae;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dp-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.dp-chip {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border-radius: 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  font-weight: 800;
  line-height: 1;
}
.dp-name {
  font-weight: 700;
  font-size: 15px;
  color: #263238;
  word-break: break-all;
}
.dp-type {
  margin-left: auto;
}
.dp-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #78909c;
}
.dp-section {
  border-top: 1px solid #eef2f6;
  padding-top: 10px;
}
.dp-sec-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  color: #546e7a;
  cursor: pointer;
  margin-bottom: 8px;
}
.dp-sec-arrow {
  font-size: 10px;
  color: #90a4ae;
}
.dp-search {
  margin-bottom: 8px;
}
.dp-col-list {
  max-height: 200px;
  overflow-y: auto;
}
.dp-col {
  display: flex;
  gap: 8px;
  padding: 4px 0;
  font-size: 12px;
  cursor: pointer;
}
.dp-col:hover {
  background: #F5F7FA;
  border-radius: 4px;
  margin-left: -4px;
  padding-left: 4px;
}
.dp-col::before {
  content: '☐';
  color: #b0bec5;
  font-size: 10px;
}
.dp-col-name {
  flex: 1;
  color: #37474f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dp-col-type {
  color: #90a4ae;
  font-size: 11px;
}
.dp-neighbors {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 220px;
  overflow-y: auto;
}
.dp-node-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 9px;
  border: 1px solid #e8eaf0;
  border-left: 3px solid #4a90e2;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  transition: box-shadow 0.12s, border-color 0.12s;
}
.dp-node-card:hover {
  box-shadow: 0 2px 8px rgba(74, 144, 226, 0.18);
  border-color: #4a90e2;
}
.dp-nb-dir {
  flex-shrink: 0;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 3px;
}
.dp-nb-dir.up {
  background: #ecfdf5;
  color: #10b981;
}
.dp-nb-dir.down {
  background: #e3f2fd;
  color: #4a90e2;
}
.dp-nb-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: #37474f;
}
.dp-nb-remove {
  opacity: 0;
  margin-left: auto;
  transition: opacity .15s;
}
.dp-node-card:hover .dp-nb-remove {
  opacity: 1;
}
.dp-empty {
  padding: 8px 4px;
  font-size: 12px;
  color: #90a4ae;
}
.dp-actions {
  display: flex;
  gap: 8px;
  margin-top: 2px;
}
.dp-dq {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  margin-left: 8px;
}
.dp-dq.dq-none { background: #94A3B8; }
.dp-dq.dq-high { background: #10B981; }
.dp-dq.dq-mid  { background: #F59E0B; }
.dp-dq.dq-low  { background: #EF4444; }

.tb-label {
  font-size: 12px;
  color: #6B7280;
  margin-right: 6px;
}

.dialog-hint {
  color: #6B7280;
  font-size: 12px;
  margin: 0 0 10px;
}

.path-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.path-selects {
  display: flex;
  gap: 12px;
}
.path-field {
  flex: 1;
}
.path-label {
  display: block;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}
.path-dialog-actions {
  display: flex;
  justify-content: flex-end;
}
.path-result {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.path-chain {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
.chain-arrow {
  color: #9ca3af;
  font-size: 12px;
}
.path-hops h4 {
  margin: 8px 0;
  font-size: 13px;
  color: #6b7280;
}

.sql-dialog-hint {
  color: #6b7280;
  font-size: 12px;
  margin: 0 0 8px;
}
.sql-dialog-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}
.sql-result {
  margin-top: 14px;
  border-top: 1px solid #e5e7eb;
  padding-top: 12px;
}
.sql-result-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.sql-result-arrow {
  font-size: 13px;
  color: #374151;
}
.sql-result h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #6b7280;
}
.dp-add-link { margin-top: 8px; width: 100%; }
</style>
