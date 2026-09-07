<template>
  <div class="lineage-v2-page">
    <!-- Screen-reader live region for operation feedback (06 §6.1). -->
    <div ref="ariaRef" aria-live="polite" aria-atomic="true" class="sr-only"></div>

    <!-- ============ [B] Asset tree ============ -->
    <aside class="lv2-tree" role="navigation" aria-label="资产树">
      <LgAssetTree
        :tree="assetTree"
        :recent="store.recentNodes"
        :bookmarks="store.bookmarks"
        :active-id="String(store.rootNodeId || '')"
        :collapsed="store.treeCollapsedNodes"
        :root-node-id="String(store.rootNodeId || '')"
        @select="onTreeSelect"
        @recent-click="(r) => store.selectNode(r.id)"
        @bookmark-click="onBookmarkRestore"
        @bookmark-remove="(id) => store.removeBookmark(id)"
        @add-bookmark="onAddBookmark"
        @toggle-collapse="onToggleCollapse"
      />
    </aside>

    <!-- ============ [A] + [C]/[D] main column ============ -->
    <div class="lv2-main">
      <header class="lv2-toolbar" aria-label="血缘工具栏">
        <section aria-label="Load start">
          <el-select
            v-model="searchKeyword"
            filterable
            remote
            :remote-method="handleSearch"
            :loading="searching"
            :placeholder="t('lineage.toolbar.rootSearch.placeholder')"
            clearable
            size="small"
            class="tb-search"
            ref="rootSearchRef"
            @change="(id) => id && store.selectNode(id)"
            @visible-change="(v) => (rootSearchOpen = v)"
          >
            <el-option v-for="t1 in searchResults" :key="t1.id" :label="t1.name" :value="t1.id">
              <span class="so-name">{{ t1.name }}</span>
              <el-tag :type="nodeTypeTag(t1.nodeType)" size="small">{{ t1.nodeType }}</el-tag>
            </el-option>
          </el-select>
          <el-button :icon="RefreshRight" size="small" :title="t('lineage.toolbar.actions.addEdge')" @click="rebuildGraph">重建</el-button>
          <el-button
            :icon="Share"
            size="small"
            :type="store.isOverview ? 'primary' : ''"
            :disabled="store.isOverview"
            @click="store.loadOverview()"
          >概览</el-button>
        </section>
        <el-divider direction="vertical" class="tb-sep" />

        <!-- ② Graph level -->
        <section aria-label="Graph level">
          <el-radio-group v-model="store.graphLevel" size="small">
            <el-radio-button value="table">{{ t('lineage.toolbar.level.table') }}</el-radio-button>
            <el-radio-button value="column">{{ t('lineage.toolbar.level.column') }}</el-radio-button>
          </el-radio-group>
        </section>
        <el-divider direction="vertical" class="tb-sep" />

        <!-- ③ Multi-dim filter -->
        <section aria-label="多维过滤">
          <el-select v-model="filterDomain" size="small" clearable :placeholder="t('lineage.toolbar.filter.domain')" class="tb-f" filterable>
            <el-option v-for="d in domainOptions" :key="d" :label="d" :value="d" />
          </el-select>
          <el-select v-model="filterOwner" size="small" clearable :placeholder="t('lineage.toolbar.filter.owner')" class="tb-f" filterable>
            <el-option v-for="o in ownerOptions" :key="o" :label="o" :value="o" />
          </el-select>
          <el-select v-model="filterTag" size="small" clearable :placeholder="t('lineage.toolbar.filter.tag')" class="tb-f" filterable>
            <el-option v-for="t2 in tagOptions" :key="t2" :label="t2" :value="t2" />
          </el-select>
          <el-select v-model="filterColumn" size="small" filterable clearable :placeholder="t('lineage.toolbar.filter.column')" class="tb-f" @change="colFilterChanged">
            <el-option v-for="c in columnOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </section>
        <el-divider direction="vertical" class="tb-sep" />

        <!-- ④ Filter mode -->
        <section aria-label="Filter mode">
          <el-radio-group v-model="filterMode" size="small">
            <el-radio-button value="dim">{{ t('lineage.toolbar.mode.dim') }}</el-radio-button>
            <el-radio-button value="hide">{{ t('lineage.toolbar.mode.hide') }}</el-radio-button>
          </el-radio-group>
        </section>
        <el-divider direction="vertical" class="tb-sep" />

        <!-- ⑤ Direction / depth / node type -->
        <section aria-label="Direction depth node type">
          <el-select v-model="store.graphDirection" size="small" class="tb-d">
            <el-option :label="t('lineage.toolbar.dir.both')" value="both" />
            <el-option :label="t('lineage.toolbar.dir.up')" value="up" />
            <el-option :label="t('lineage.toolbar.dir.down')" value="down" />
          </el-select>
          <el-select v-model="store.graphDepth" size="small" class="tb-d">
            <el-option v-for="d in [1,2,3,4,5]" :key="d" :label="`深度 ${d}`" :value="d" />
          </el-select>
          <el-select v-model="store.nodeTypeFilter" size="small" clearable placeholder="节点类型" class="tb-d">
            <el-option label="表" value="TABLE" />
            <el-option label="字段" value="COLUMN" />
            <el-option label="任务" value="TASK" />
          </el-select>
          <el-button link size="small" :disabled="!hasFilter" @click="clearAllFilters">Clear All</el-button>
        </section>
        <el-divider direction="vertical" class="tb-sep" />

        <!-- ⑥ Layers / layout / ring detection -->
        <section aria-label="图层切换">
          <span class="tb-label">图层</span>
          <el-checkbox-button :model-value="store.layers.entity" class="tb-chip" :label="'entity'" @change="store.toggleLayer('entity')">实体</el-checkbox-button>
          <el-checkbox-button :model-value="store.layers.column" class="tb-chip" :label="'column'" :disabled="!store.layers.entity && graphNodes.length > 200" @change="store.toggleLayer('column')">字段</el-checkbox-button>
          <el-checkbox-button :model-value="store.layers.dq" class="tb-chip" :label="'dq'" @change="store.toggleLayer('dq')">质量</el-checkbox-button>
          <el-radio-group v-model="store.layoutMode" size="small">
            <el-radio-button value="force">力</el-radio-button>
            <el-radio-button value="layer">层</el-radio-button>
            <el-radio-button value="radial">径</el-radio-button>
          </el-radio-group>
          <el-button :icon="DataAnalysis" size="small" :disabled="!store.graphData" @click="runGraphAnalysis">{{ t('lineage.toolbar.cycleDetect') }}</el-button>
        </section>
        <el-divider direction="vertical" class="tb-sep" />

        <!-- ⑦ Action group -->
        <section aria-label="Actions">
          <el-button :icon="Guide" size="small" :disabled="!store.graphData" @click="openPathDialog">{{ t('lineage.toolbar.actions.path') }}</el-button>
          <el-button :icon="Odometer" size="small" @click="loadHeat">{{ t('lineage.toolbar.actions.heat') }}</el-button>
          <el-button :icon="Document" size="small" @click="openSqlDialog">{{ t('lineage.toolbar.actions.sql') }}</el-button>
          <el-button
            :icon="Link"
            size="small"
            :disabled="!canWrite"
            :aria-disabled="!canWrite"
            :title="canWrite ? t('lineage.toolbar.actions.addEdge') : t('lineage.perm.writeRequired')"
            @click="openAssetEdgeDialog"
          >{{ t('lineage.toolbar.actions.addEdge') }}</el-button>
          <el-dropdown @command="handleExportCommand" :disabled="!store.graphData">
            <el-button size="small" :icon="Download">
              {{ t('lineage.toolbar.export.label') }}<el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="png">{{ t('lineage.toolbar.export.png') }}</el-dropdown-item>
                <el-dropdown-item command="svg">{{ t('lineage.toolbar.export.svg') }}</el-dropdown-item>
                <el-dropdown-item command="csv">{{ t('lineage.toolbar.export.csv') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-tag :type="store.wsConnected ? 'success' : 'info'" size="small">{{ store.wsConnected ? t('lineage.status.connected') : t('lineage.status.offline') }}</el-tag>
        </section>
      </header>

      <div class="lv2-body">
        <!-- ============ [C] Canvas ============ -->
        <section class="lv2-canvas" aria-label="血缘画布">
          <!-- Empty state -->
          <LgEmptyState
            v-if="!store.graphData && !store.graphLoading"
            :recent="store.recentNodes"
            :bookmarks="store.bookmarks"
            @select="onEmptySelect"
            @recent-click="(r) => store.selectNode(r.id)"
            @bookmark-click="onBookmarkRestore"
            @use-search="focusRootSearch"
            @paste-fqn="onPasteFqn"
          />
          <!-- Loading skeleton -->
          <LgLoadingSkeleton v-else-if="store.graphLoading" />

          <!-- Graph -->
          <template v-else>
            <div v-show="!rootSearchOpen" class="lv2-graph-stats">
              <span v-if="store.graphData?.rootTableName">根：<strong>{{ store.graphData.rootTableName }}</strong></span>
              <el-tag size="small" type="success">节点 {{ store.graphData?.nodes?.length || 0 }}</el-tag>
              <el-tag size="small">连线 {{ store.graphData?.links?.length || 0 }}</el-tag>
            </div>
            <div ref="canvasRef" tabindex="-1" class="lv2-graph-focus">
              <LineageGraph
                v-if="store.graphData"
                ref="g6Ref"
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
                @node-dragend="onNodeDragEnd"
                @update:screenshot-lock="(v) => (screenshotLock = v)"
                @edge-edit="onEdgeEdit"
                @edge-delete="onEdgeDelete"
              />
            </div>
          </template>
        </section>

        <!-- ============ [D] Detail panel (3-section rework) ============ -->
        <aside class="lv2-detail" role="complementary" aria-label="资产详情">
          <template v-if="store.selectedNode">
            <!-- §1 Asset metadata -->
            <div class="lv2-detail-head">
              <span class="lv2-detail-chip" :style="{ background: nodeChip(store.selectedNode).bg, color: nodeChip(store.selectedNode).fg }">{{ nodeChip(store.selectedNode).abbr }}</span>
              <span class="lv2-detail-name" :title="store.selectedNode.name">{{ store.selectedNode.name }}</span>
              <el-tag :type="nodeTypeTag(store.selectedNode.nodeType)" size="small">{{ store.selectedNode.nodeType }}</el-tag>
              <LgDqBadge
                v-if="store.layers.dq"
                :score="store.dqScoreForId?.(store.selectedNode.id) ?? null"
                :compact="true"
                @click-detail="onDqDetail"
              />
            </div>
            <div class="lv2-detail-meta">
              <span class="lv2-detail-cell">类型：{{ store.selectedNode.nodeType || '–' }}</span>
              <span class="lv2-detail-cell">Domain：{{ store.selectedNode.data?.domain || '–' }}</span>
              <span class="lv2-detail-cell">Owner：{{ store.selectedNode.data?.owner || '–' }}</span>
              <span class="lv2-detail-cell">Tags：
                <template v-if="selTags.length">
                  <el-tag v-for="tg in visibleTags" :key="tg" size="small" class="lv2-tag">{{ tg }}</el-tag>
                  <span v-if="selTags.length > 3" class="lv2-tag-more">+{{ selTags.length - 3 }}</span>
                </template>
                <span v-else>–</span>
              </span>
            </div>

            <!-- Governance info: collapsible -->
            <button type="button" class="lv2-gov-toggle" :aria-expanded="detailGovOpen" @click="detailGovOpen = !detailGovOpen">
              {{ detailGovOpen ? '▲ 收起治理信息' : '▼ 展开治理信息' }}
            </button>
            <div v-if="detailGovOpen" class="lv2-gov">
              <div class="lv2-gov-row"><span class="lv2-gov-k">FQN</span><LgCopyFqn :text="selFqn" size="sm" @copied="onCopied" /></div>
              <div class="lv2-gov-row"><span class="lv2-gov-k">更新时间</span><span class="lv2-gov-v">{{ selNodeMeta.updatedAt || '–' }}</span></div>
              <div class="lv2-gov-row"><span class="lv2-gov-k">最后编辑者</span><span class="lv2-gov-v">{{ selNodeMeta.lastEditor || '–' }}</span></div>
              <div class="lv2-gov-row"><span class="lv2-gov-k">血缘版本</span><span class="lv2-gov-v">{{ selNodeMeta.version || '–' }}</span></div>
            </div>

            <!-- §2 Columns -->
            <div class="lv2-detail-sec">
              <div class="lv2-detail-sec-title">{{ t('lineage.dp.section.columns').replace('{count}', columns.length) }}</div>
              <LgColumnList
                v-if="columns.length"
                :columns="columns"
                :highlight="hlCol"
                :search-text="columnSearchText"
                @update:search-text="(v) => (columnSearchText = v)"
                @select-column="onColumnSelect"
                @sort-change="(p) => void p"
              />
              <div v-else class="lv2-empty">该节点暂无字段映射</div>
            </div>

            <!-- §3 Connected neighbors -->
            <div class="lv2-detail-sec">
              <div class="lv2-detail-sec-title">{{ t('lineage.dp.section.neighbors') }}</div>
              <LgStatusTab :tabs="neighborTabs" v-model="neighborDirTab" aria-label="关联节点方向" />
              <div v-if="filteredNeighbors.length" class="lv2-neighbors">
                <LgNeighborCard
                  v-for="nb in filteredNeighbors"
                  :key="nb.id"
                  :node-id="nb.id"
                  :name="nb.name"
                  :node-type="nb.nodeType"
                  :dir="nb.dir"
                  :can-remove="canWrite"
                  @focus="onNeighborFocus"
                  @remove="onNeighborRemove"
                />
              </div>
              <div v-else class="lv2-empty">无直接关联节点</div>
            </div>

            <!-- ＋ Add related node -->
            <el-button
              size="small"
              type="primary"
              plain
              :disabled="!canWrite"
              :aria-disabled="!canWrite"
              :title="canWrite ? t('lineage.dp.neighbors.add') : t('lineage.perm.writeRequired')"
              @click="openAddEdgeDialog"
            >{{ t('lineage.dp.neighbors.add') }}</el-button>

            <!-- Bottom action bar -->
            <div class="lv2-detail-actions">
              <LgCopyFqn :text="selFqn" :size="'sm'" @copied="onCopied" />
              <el-button size="small" @click="store.clearOverrides()">清除位置覆盖</el-button>
              <el-button type="primary" size="small" @click="store.refocus(store.selectedNode.id)">以此为中心</el-button>
              <el-button size="small" @click="store.clearSelection()">关闭</el-button>
            </div>
          </template>
          <div v-else class="lv2-empty-detail">选中画布节点查看详情</div>
        </aside>
      </div>
    </div>

    <!-- ============ Statusbar ============ -->
    <footer class="lv2-status" aria-label="状态栏">
      <span class="st-item">
        <el-tag :type="store.wsConnected ? 'success' : 'info'" size="small">{{ store.wsConnected ? t('lineage.status.connected') : t('lineage.status.offline') }}</el-tag>
      </span>
      <span class="st-item">节点：{{ store.renderStats.nodes }}</span>
      <span class="st-item">边：{{ store.renderStats.edges }}</span>
      <span class="st-item">渲染 {{ store.renderStats.ms }}ms</span>
      <span class="st-spacer"></span>
      <span class="st-chip st-read">LINEAGE:READ</span>
      <span v-if="canWrite" class="st-chip st-write">LINEAGE:WRITE</span>
    </footer>

    <!-- ============ Dialogs ============ -->
    <el-dialog v-model="pathDialogVisible" :title="t('lineage.toolbar.actions.path')" width="620px" destroy-on-close>
      <div class="dlg-body">
        <div class="dlg-row">
          <span class="dlg-label">起点</span>
          <el-select v-model="pathSource" filterable remote :remote-method="(k) => searchFor(k, 'pathSource')" :loading="searching" placeholder="搜索起点" style="width:100%">
            <el-option v-for="t in pathSourceResults" :key="t.id" :label="`${t.name} (${t.nodeType})`" :value="t.id" />
          </el-select>
        </div>
        <div class="dlg-row">
          <span class="dlg-label">终点</span>
          <el-select v-model="pathTarget" filterable remote :remote-method="(k) => searchFor(k, 'pathTarget')" :loading="searching" placeholder="搜索终点" style="width:100%">
            <el-option v-for="t in pathTargetResults" :key="t.id" :label="`${t.name} (${t.nodeType})`" :value="t.id" />
          </el-select>
        </div>
        <div class="dlg-row">
          <el-button type="primary" :loading="pathLoading" @click="runPathAnalysis">查找最短路径</el-button>
        </div>
        <div v-if="pathResult" class="dlg-result">
          <el-alert :type="pathResult.found ? 'success' : 'warning'" :closable="false"
            :title="pathResult.found ? `找到路径（${pathResult.path.length} 个节点）` : '未找到连通路径'" />
          <div v-if="pathResult.found" class="dlg-chain">
            <template v-for="(id, i) in pathResult.path" :key="i">
              <span v-if="i > 0" class="dlg-arrow">→</span>
              <el-tag size="small">{{ pathName(id) }}</el-tag>
            </template>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="heatDialogVisible" title="热度排行（TOP 出入度）" width="560px" destroy-on-close>
      <el-table :data="heatData" size="small" border max-height="460" v-loading="heatLoading">
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

    <el-dialog v-model="sqlDialogVisible" title="SQL 血缘解析" width="680px" destroy-on-close>
      <div class="dlg-body">
        <el-input v-model="sqlText" type="textarea" :rows="5" placeholder="INSERT...SELECT / CREATE TABLE AS SELECT…" />
        <div class="dlg-row">
          <el-button type="primary" :loading="sqlAnalyzing" @click="runSqlAnalysis">{{ t('lineage.toolbar.actions.sql') }}</el-button>
        </div>
        <div v-if="sqlResult" class="dlg-result">
          <el-tag type="primary">{{ sqlResult.sqlType }}</el-tag>
          <span class="dlg-arrow">{{ (sqlResult.inputTables || []).join(', ') || '-' }} → {{ sqlResult.outputTable || '(查询)' }}</span>
          <el-table v-if="sqlResult.fieldMappings?.length" :data="sqlResult.fieldMappings" size="small" border max-height="220" class="dlg-table">
            <el-table-column prop="sourceTable" label="来源表" width="150" show-overflow-tooltip />
            <el-table-column prop="sourceColumn" label="来源列" width="130" show-overflow-tooltip />
            <el-table-column prop="targetColumn" label="目标列" width="130" show-overflow-tooltip />
            <el-table-column prop="transformation" label="转换逻辑" min-width="160" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
    </el-dialog>

    <!-- ===== New-edge dialog: global assetEdge (schema-driven) ===== -->
    <el-dialog
      v-model="assetEdgeVisible"
      :title="t('lineage.dialog.assetEdge.title')"
      width="600px"
      destroy-on-close
      :aria-labelledby="'asset-edge-title'"
      :aria-describedby="'asset-edge-hint'"
    >
      <span id="asset-edge-title" class="sr-only">{{ t('lineage.dialog.assetEdge.title') }}</span>
      <p id="asset-edge-hint" class="lg-dlg-hint">Choose source and target assets to create a lineage edge.</p>
      <el-form :model="assetEdgeForm" :rules="edgeFormRules" ref="assetEdgeFormRef" label-width="90px" size="default">
        <LgEdgeFormFields :fields="assetEdgeFields" :form="assetEdgeForm" :results-map="searchResultsMap" :searching="searching" :label-fn="enumLabel" @search="onFieldSearch" />
      </el-form>
      <template #footer>
        <LgDialogActionBar saveText="保存并刷新" cancelText="取消" :loading="submittingEdge" @save="submitAssetEdge" @cancel="assetEdgeVisible = false" />
      </template>
    </el-dialog>

    <!-- ===== New-edge dialog: addEdge (presets source to current node) ===== -->
    <el-dialog
      v-model="addEdgeVisible"
      :title="t('lineage.dialog.addEdge.title')"
      width="600px"
      destroy-on-close
      :aria-labelledby="'add-edge-title'"
      :aria-describedby="'add-edge-hint'"
    >
      <span id="add-edge-title" class="sr-only">{{ t('lineage.dialog.addEdge.title') }}</span>
      <p id="add-edge-hint" class="lg-dlg-hint">Add an outbound lineage edge from the selected node.</p>
      <el-form :model="addEdgeForm" :rules="edgeFormRules" ref="addEdgeFormRef" label-width="90px" size="default">
        <LgEdgeFormFields :fields="addEdgeFields" :form="addEdgeForm" :results-map="searchResultsMap" :searching="searching" :label-fn="enumLabel" @search="onFieldSearch" />
      </el-form>
      <template #footer>
        <LgDialogActionBar saveText="保存并刷新" cancelText="取消" :loading="submittingEdge" @save="submitAddEdge" @cancel="addEdgeVisible = false" />
      </template>
    </el-dialog>

    <!-- ===== Edit-edge dialog (PATCH via patchEdge) ===== -->
    <el-dialog
      v-model="editEdgeVisible"
      :title="t('lineage.dialog.editEdge.title')"
      width="600px"
      destroy-on-close
      :aria-labelledby="'edit-edge-title'"
      :aria-describedby="'edit-edge-hint'"
    >
      <span id="edit-edge-title" class="sr-only">{{ t('lineage.dialog.editEdge.title') }}</span>
      <p id="edit-edge-hint" class="lg-dlg-hint">Source and target are fixed; edit the relation metadata below.</p>
      <el-form :model="editEdgeForm" :rules="edgeFormRules" ref="editEdgeFormRef" label-width="90px" size="default">
        <el-form-item label="源节点"><el-input :model-value="editEdgeForm.sourceId" disabled /></el-form-item>
        <el-form-item label="目标节点"><el-input :model-value="editEdgeForm.targetId" disabled /></el-form-item>
        <LgEdgeFormFields :fields="editEdgeFields" :form="editEdgeForm" :results-map="searchResultsMap" :searching="searching" :label-fn="enumLabel" @search="onFieldSearch" />
      </el-form>
      <template #footer>
        <LgDialogActionBar saveText="保存修改" cancelText="取消" :loading="submittingEdge" @save="submitEditEdge" @cancel="editEdgeVisible = false" />
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * LineageV2 — Phase-A 4-block layout plus Phase-B optimisations:
 *   [A] Toolbar 7 分组 · [B] LgAssetTree · [C] Canvas (empty/loading/graph)
 *   [D] 3-section detail panel (§1 governance / §2 columns / §3 neighbors)
 *   3 schema-driven write dialogs (assetEdge / addEdge / editEdge)
 *   6 global keyboard shortcuts · bookmarks save/restore · i18n/a11y/theme.
 */
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Share, RefreshRight, DataAnalysis, Guide, Odometer, Document, Link, Download, ArrowRight } from '@element-plus/icons-vue'
import { useLineageStore } from '@/stores/lineage'
import { lineageApi } from '@/api/modules/lineage'
import { hasPerm } from '@/lib/permission'
import { resolveSource } from '@/components/lineage/vueflow/sourceLogoRegistry'
import LineageGraph from '@/components/lineage/LineageGraph.vue'
import LgAssetTree from '@/components/lineage/LgAssetTree.vue'
import LgEmptyState from '@/components/lineage/LgEmptyState.vue'
import LgLoadingSkeleton from '@/components/lineage/LgLoadingSkeleton.vue'
import LgDqBadge from '@/components/lineage/LgDqBadge.vue'
import LgCopyFqn from '@/components/lineage/LgCopyFqn.vue'
import LgColumnList from '@/components/lineage/LgColumnList.vue'
import LgStatusTab from '@/components/lineage/LgStatusTab.vue'
import LgNeighborCard from '@/components/lineage/LgNeighborCard.vue'
import LgDialogActionBar from '@/components/lineage/LgDialogActionBar.vue'
import LgEdgeFormFields from '@/components/lineage/LgEdgeFormFields.vue'
import {
  edgeFormMeta,
  edgeFormRules,
  EDGE_ENUM_LABELS,
  emptyEdgeForm,
  edgeAttrsFrom,
  setEdgeForm
} from '@/composables/useLineageEdgeFormSchema'
import { useLineageKeyboard } from '@/composables/useLineageKeyboard'
import zhCN from '@/locales/lineage.zh-CN.js'
import enUS from '@/locales/lineage.en-US.js'

const store = useLineageStore()

// ---------- Minimal i18n helper (no vue-i18n dependency) ----------
const dicts = { 'zh-CN': zhCN, 'en-US': enUS }
function currentLocale() {
  try { return localStorage.getItem('td_locale') === 'en-US' ? 'en-US' : 'zh-CN' } catch (_e) { return 'zh-CN' }
}
const dict = dicts[currentLocale()] || zhCN
function t(key) { return dict[key] != null ? dict[key] : key }

// ---------- Permission & UI state ----------
const canWrite = computed(() => hasPerm('LINEAGE:WRITE'))
const screenshotLock = ref(false)
const g6Ref = ref(null)
const rootSearchRef = ref(null)
const rootSearchOpen = ref(false)
const canvasRef = ref(null)
const ariaRef = ref(null)
const cycleEdgeKeys = ref([])
const searchKeyword = ref('')

// ---------- Advanced filters ----------
const filterDomain = ref(null)
const filterOwner = ref(null)
const filterTag = ref(null)
const filterColumn = ref(null)
const filterMode = ref('dim')

const graphNodes = computed(() => store.graphData?.nodes || [])

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
  for (const l of (store.graphData?.links || [])) {
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

const centerNodeId = computed(() => store.rootNodeId || graphNodes.value.find(n => n.category === 'root')?.id)

const hasFilter = computed(() =>
  filterDomain.value != null || filterOwner.value != null || filterTag.value != null || filterColumn.value != null
)

function clearAllFilters() {
  filterDomain.value = null
  filterOwner.value = null
  filterTag.value = null
  filterColumn.value = null
  store.clearColumnHighlight()
}

// viewGraphData / filterKeepIds / filterDimEnabled — same semantics as before.
const viewGraphData = computed(() => {
  const raw = store.graphData
  if (!raw) return raw
  if (filterMode.value !== 'hide' || !hasFilter.value) return raw
  const nodes = raw.nodes || []
  const links = raw.links || []
  const colFilter = filterColumn.value
  function nodePassesFilter(n) {
    const nd = n.data ?? n.attributes ?? {}
    if (filterDomain.value != null && (nd.domain ?? n.domain) !== filterDomain.value) return false
    if (filterOwner.value != null && (nd.owner ?? n.owner) !== filterOwner.value) return false
    if (filterTag.value != null) {
      const tags = nd.tags ?? n.tags ?? []
      if (!tags.includes(filterTag.value)) return false
    }
    return true
  }
  const passIds = new Set()
  for (const n of nodes) {
    if (nodePassesFilter(n)) passIds.add(n.id)
  }
  if (colFilter) {
    for (const l of links) {
      if ((l.fieldLinks || []).some(fl => fl.sourceColumn === colFilter || fl.targetColumn === colFilter)) {
        passIds.add(l.source); passIds.add(l.target)
      }
    }
  }
  const keepIds = new Set(passIds)
  for (const l of links) {
    if (passIds.has(l.source)) keepIds.add(l.target)
    if (passIds.has(l.target)) keepIds.add(l.source)
  }
  return { ...raw, nodes: nodes.filter(n => keepIds.has(n.id)), links: links.filter(l => keepIds.has(l.source) && keepIds.has(l.target)) }
})

const filterKeepIds = computed(() => {
  const raw = store.graphData
  if (!raw || !hasFilter.value) return []
  const nodes = raw.nodes || []
  const links = raw.links || []
  const colFilter = filterColumn.value
  const base = new Set()
  for (const n of nodes) {
    const d = n.data || n.attributes || n
    const tags = d.tags || d.tagList || []
    if (filterDomain.value != null && (d.domain ?? d.Domain) !== filterDomain.value) continue
    if (filterOwner.value != null && (d.owner ?? d.Owner) !== filterOwner.value) continue
    if (filterTag.value != null && !tags.includes(filterTag.value)) continue
    base.add(n.id)
  }
  if (colFilter) {
    for (const l of links) {
      if ((l.fieldLinks || []).some(fl => fl.sourceColumn === colFilter || fl.targetColumn === colFilter)) { base.add(l.source); base.add(l.target) }
    }
  }
  if (!base.size) return []
  const keep = new Set(base)
  for (const l of links) {
    if (base.has(l.source) || base.has(l.target)) { keep.add(l.source); keep.add(l.target) }
  }
  return [...keep]
})
const filterDimEnabled = computed(() => filterMode.value === 'dim' && filterKeepIds.value.length > 0)

// ---------- Asset tree [B] grouping ----------
const assetTree = computed(() => {
  const grp = {}
  for (const n of (store.graphData?.nodes || [])) {
    const d = n.data || n.attributes || {}
    const domain = ((d.domain ?? d.Domain) || '未知')
    const owner = ((d.owner ?? d.Owner) || '未知')
    const name = n.name || n.id
    if (!grp[domain]) grp[domain] = {}
    if (!grp[domain][owner]) grp[domain][owner] = []
    grp[domain][owner].push({ key: `D:${domain}::O:${owner}::N:${name}`, id: n.id, name, nodeType: n.nodeType })
  }
  return Object.keys(grp).map(domain => ({
    key: `D:${domain}`,
    name: domain,
    children: Object.keys(grp[domain]).map(owner => ({
      key: `D:${domain}::O:${owner}`,
      name: owner,
      children: grp[domain][owner]
    }))
  }))
})

function onToggleCollapse(key) {
  const cur = store.treeCollapsedNodes[key] === true
  store.treeCollapsedNodes = { ...store.treeCollapsedNodes, [key]: !cur }
  try {
    localStorage.setItem('td_lineage_tree_collapsed', JSON.stringify(store.treeCollapsedNodes))
  } catch (e) { /* ignore storage failures */ }
}

// ---------- Bookmarks: save / restore (B3) ----------
function onAddBookmark() {
  if (!store.rootNodeId) return
  store.addBookmark({
    id: store.rootNodeId,
    name: store.nodeNameMap[store.rootNodeId] || store.rootNodeId,
    nodeType: store.selectedNode?.nodeType || '',
    query: {
      filterDomain: filterDomain.value,
      filterOwner: filterOwner.value,
      filterTag: filterTag.value,
      filterColumn: filterColumn.value,
      filterMode: filterMode.value,
      graphLevel: store.graphLevel,
      graphDirection: store.graphDirection,
      graphDepth: store.graphDepth
    }
  })
  ElMessage.success('已收藏当前根节点')
  announce('已收藏当前根节点')
}

function tryParseJson(s) {
  if (typeof s !== 'string') return null
  try { const v = JSON.parse(s); return v && typeof v === 'object' ? v : null } catch (_e) { return null }
}

function onBookmarkRestore(b) {
  if (!b?.id) return
  const q = (b.query && typeof b.query === 'object' && !Array.isArray(b.query)) ? b.query : tryParseJson(b.query)
  if (q) {
    if (q.filterDomain != null) filterDomain.value = q.filterDomain
    if (q.filterOwner != null) filterOwner.value = q.filterOwner
    if (q.filterTag != null) filterTag.value = q.filterTag
    if (q.filterColumn != null) filterColumn.value = q.filterColumn
    if (q.filterMode) filterMode.value = q.filterMode
    if (q.graphLevel) store.graphLevel = q.graphLevel
    if (q.graphDirection) store.graphDirection = q.graphDirection
    if (q.graphDepth) store.graphDepth = q.graphDepth
  }
  store.selectNode(b.id)
}

// ---------- Empty-state handlers ----------
function onEmptySelect(node) {
  if (node?.id) store.selectNode(node.id)
}
function focusRootSearch() {
  rootSearchRef.value?.focus?.()
}
function onPasteFqn({ fqn, valid }) {
  if (!valid) {
    ElMessage.warning('FQN 格式不正确（需形如 a.b.c）')
    return
  }
  store.selectNode(fqn)
}

// ---------- Search (root + assetSearch fields) ----------
const searching = ref(false)
const searchResults = ref([])
const searchResultsMap = reactive({})
async function handleSearch(kw) {
  if (!kw) { searchResults.value = []; return }
  searching.value = true
  try {
    searchResults.value = await lineageApi.search(kw)
  } catch (e) { searchResults.value = [] } finally { searching.value = false }
}
async function searchField(kw, key) {
  if (!kw) { searchResultsMap[key] = []; return }
  searching.value = true
  try {
    searchResultsMap[key] = await lineageApi.search(kw)
  } catch (e) { searchResultsMap[key] = [] } finally { searching.value = false }
}
function onFieldSearch({ keyword, key }) {
  searchField(keyword, key)
}
async function searchFor(kw, target) {
  if (!kw) {
    if (target === 'pathSource') pathSourceResults.value = []
    else if (target === 'pathTarget') pathTargetResults.value = []
    return
  }
  searching.value = true
  try {
    const res = await lineageApi.search(kw)
    if (target === 'pathSource') pathSourceResults.value = res
    else if (target === 'pathTarget') pathTargetResults.value = res
  } catch (e) { /* ignore */ } finally { searching.value = false }
}

// ---------- Helpers ----------
function nodeChip(n) {
  if (!n) return { abbr: '?', bg: '#e0e0e0', fg: '#546e7a' }
  const d = n.data || {}
  return resolveSource(d.engine || d.datasourceName || d.attributes?.datasourceName || d.attributes?.engine || d.database || '')
}
function nodeTypeTag(t) {
  return t === 'TABLE' ? 'primary' : t === 'COLUMN' ? 'success' : t === 'TASK' ? 'warning' : 'info'
}
function pathName(id) {
  return store.nodeNameMap[id] || id
}
function colFilterChanged(val) {
  if (val) store.toggleColumnHighlight(val)
  else store.clearColumnHighlight()
}

// ---------- Detail panel §1 : governance / DQ ----------
const detailGovOpen = ref(false)
const selFqn = computed(() => store.selectedNode?.data?.fqn || store.selectedNode?.id || '')
const selNodeData = computed(() => store.selectedNode?.data || store.selectedNode?.attributes || {})
const selTags = computed(() => Array.isArray(selNodeData.value.tags) ? selNodeData.value.tags : [])
const visibleTags = computed(() => selTags.value.slice(0, 3))
const selNodeMeta = computed(() => ({
  updatedAt: selNodeData.value.updatedAt || selNodeData.value.updateTime || selNodeData.value.schemaUpdatedAt,
  lastEditor: selNodeData.value.updatedBy || selNodeData.value.lastEditor || selNodeData.value.owner,
  version: selNodeData.value.lineageVersion || selNodeData.value.version
}))
function onDqDetail(score) {
  ElMessage.info(`数据质量评分：${score == null ? '暂无' : score}`)
}
function onCopied(text) {
  ElMessage.success('已复制 ' + text)
  announce('已复制节点标识')
}

// ---------- Detail panel §2 : columns ----------
const columnSearchText = ref('')
const hlCol = ref(null)
const columns = computed(() => {
  const node = store.selectedNode
  if (!node) return []
  const d = node.data || node.attributes || {}
  const direct = d.columns && Array.isArray(d.columns) ? d.columns : (Array.isArray(node.columns) ? node.columns : null)
  if (Array.isArray(direct) && direct.length) {
    return direct.map(c => typeof c === 'string'
      ? { name: c, type: '', nullable: false, pk: false, indexed: false, source: false, target: false }
      : {
          name: c.name || c.column || '',
          type: c.type || '',
          nullable: !!c.nullable,
          pk: !!c.pk,
          indexed: !!c.indexed,
          source: false,
          target: false
        }).filter(c => c.name)
  }
  if (node.nodeType === 'COLUMN') {
    return [{ name: node.name, type: d.type || '', nullable: false, pk: false, indexed: false, source: true, target: true }]
  }
  const srcSet = new Set()
  const tgtSet = new Set()
  for (const l of (store.graphData?.links || [])) {
    if (l.source !== node.id && l.target !== node.id) continue
    for (const fl of (l.fieldLinks || [])) {
      if (fl.sourceColumn) srcSet.add(fl.sourceColumn)
      if (fl.targetColumn) tgtSet.add(fl.targetColumn)
    }
  }
  const names = new Set([...srcSet, ...tgtSet])
  return [...names].map(name => ({
    name,
    type: '',
    nullable: false,
    pk: false,
    indexed: false,
    source: srcSet.has(name),
    target: tgtSet.has(name)
  }))
})
function onColumnSelect(col) {
  const name = col?.name
  if (!name) return
  if (hlCol.value === name) {
    hlCol.value = null
    store.clearColumnHighlight()
  } else {
    hlCol.value = name
    store.toggleColumnHighlight(name)
  }
}

// ---------- Detail panel §3 : neighbors ----------
const neighborDirTab = ref('all')
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
    name: store.nodeNameMap[nid] || nid,
    nodeType: graphNodes.value.find(n => n.id === nid)?.nodeType || '',
    dir: map[nid],
    _ord: order[nid]
  })).sort((a, b) => a._ord - b._ord)
})
const neighborTabs = computed(() => [
  { key: 'all', label: '全部', count: neighbors.value.length },
  { key: 'up', label: '← 上游', count: neighbors.value.filter(n => n.dir === 'up').length },
  { key: 'down', label: '→ 下游', count: neighbors.value.filter(n => n.dir === 'down').length }
])
const filteredNeighbors = computed(() => {
  if (neighborDirTab.value === 'all') return neighbors.value
  return neighbors.value.filter(n => n.dir === neighborDirTab.value)
})
function onNeighborFocus(id) {
  store.refocus(id)
}
function onNeighborRemove({ nodeId, dir }) {
  removeNeighborLink({ id: nodeId, dir, name: store.nodeNameMap[nodeId] || nodeId })
}

// ---------- Node interaction & rebuild ----------
function onNodeClick(node) { store.selectedNode = node }
function onNodeRefocus(node) { store.refocus(node.id); ElMessage.success(`已聚焦节点：${node.name}`) }
function onNodeDragEnd(ev) { store.setOverride(ev.nodeId, ev.x, ev.y); ElMessage.success('已暂存该节点位置', 2000) }

async function rebuildGraph() {
  try {
    await ElMessageBox.confirm('将全量重建血缘图数据（约数十秒）。确认继续？', '重建图数据', { type: 'warning' })
  } catch (e) { return }
  store.graphLoading = true
  try {
    const res = await lineageApi.rebuild()
    ElMessage.success(`重建完成：${res.totalNodes} 节点 / ${res.totalEdges} 边`)
    if (store.rootNodeId) await store.loadGraph()
    else await store.loadOverview()
  } catch (e) { ElMessage.error('重建失败: ' + (e.message || '未知错误')) } finally { store.graphLoading = false }
}

// ---------- Ring detection ----------
async function runGraphAnalysis() {
  if (!store.graphData) return
  try {
    const res = await lineageApi.findCycles()
    const keys = new Set()
    for (const cycle of res.cycles || []) {
      for (let i = 0; i < cycle.length - 1; i++) {
        const srcName = cycle[i]
        const tgtName = cycle[i + 1]
        const srcId = Object.keys(store.nodeNameMap).find(k => store.nodeNameMap[k] === srcName)
        const tgtId = Object.keys(store.nodeNameMap).find(k => store.nodeNameMap[k] === tgtName)
        if (srcId && tgtId) keys.add(`${srcId}|${tgtId}`)
      }
    }
    cycleEdgeKeys.value = [...keys]
    ElMessageBox.alert(
      res.summary + (res.cycles.length ? '\n环：' + res.cycles.map(c => c.join(' → ')).join('\n') : ''),
      '环检测结果', { confirmButtonText: '知道了', type: res.hasCycle ? 'warning' : 'success' }
    )
    if (!res.hasCycle) cycleEdgeKeys.value = []
  } catch (e) { ElMessage.error('环检测失败: ' + (e.message || '未知错误')) }
}

// ---------- Path ----------
const pathDialogVisible = ref(false)
const pathSource = ref('')
const pathTarget = ref('')
const pathSourceResults = ref([])
const pathTargetResults = ref([])
const pathLoading = ref(false)
const pathResult = ref(null)
function openPathDialog() {
  pathSource.value = store.rootNodeId || ''
  pathTarget.value = ''
  pathResult.value = null
  pathDialogVisible.value = true
}
async function runPathAnalysis() {
  if (!pathSource.value || !pathTarget.value) { ElMessage.warning('请选择起点和终点'); return }
  pathLoading.value = true
  try {
    pathResult.value = await lineageApi.findPath({ sourceId: pathSource.value, targetId: pathTarget.value })
  } catch (e) { ElMessage.error('路径分析失败: ' + (e.message || '未知错误')) } finally { pathLoading.value = false }
}

// ---------- Heat ----------
const heatDialogVisible = ref(false)
const heatLoading = ref(false)
const heatData = ref([])
async function loadHeat() {
  heatDialogVisible.value = true
  heatLoading.value = true
  try {
    heatData.value = await lineageApi.getHeat()
  } catch (e) { ElMessage.error('获取热度失败: ' + (e.message || '未知错误')) } finally { heatLoading.value = false }
}

// ---------- SQL ----------
const sqlDialogVisible = ref(false)
const sqlText = ref('')
const sqlAnalyzing = ref(false)
const sqlResult = ref(null)
function openSqlDialog() {
  sqlText.value = ''
  sqlResult.value = null
  sqlDialogVisible.value = true
}
async function runSqlAnalysis() {
  if (!sqlText.value || !sqlText.value.trim()) { ElMessage.warning('请输入 SQL'); return }
  sqlAnalyzing.value = true
  try {
    const p = await lineageApi.parseSql(sqlText.value)
    sqlResult.value = {
      sqlType: p.sqlType,
      inputTables: p.sources || [],
      outputTable: p.targetTable,
      fieldMappings: (p.columnLineage || []).map(cl => ({
        sourceTable: cl.sourceTable, sourceColumn: cl.sourceColumn,
        targetColumn: cl.targetColumn, transformation: cl.transformation
      }))
    }
  } catch (e) { ElMessage.error('SQL 解析失败: ' + (e.message || '未知错误')) } finally { sqlAnalyzing.value = false }
}

// ---------- Export ----------
function toCsv(rows) {
  if (!rows || !rows.length) return ''
  const headers = Object.keys(rows[0])
  const q = (v) => { const s = String(v == null ? '' : v); return /[",\n\r]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s }
  return headers.map(q).join(',') + '\n' + rows.map(r => headers.map(h => q(r[h])).join(',')).join('\n')
}
function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = filename; a.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
async function doExportPng() {
  const dataUrl = await g6Ref.value?.exportPng()
  if (!dataUrl) { ElMessage.warning('导出失败'); return }
  const ts = new Date().toISOString().replace(/[:.]/g, '-')
  const a = document.createElement('a')
  a.href = dataUrl; a.download = `lineage_${store.graphData?.rootTableName || 'graph'}_${ts}.png`; a.click()
}
async function doExportSvg() {
  const instance = g6Ref.value?.getInstance?.()
  const dataUrl = instance?.toDataURL ? instance.toDataURL('image/svg+xml') : null
  if (!dataUrl) { ElMessage.warning('当前图实例不支持 SVG 导出，请改用 PNG'); return }
  const ts = new Date().toISOString().replace(/[:.]/g, '-')
  const a = document.createElement('a')
  a.href = dataUrl; a.download = `lineage_${store.graphData?.rootTableName || 'graph'}_${ts}.svg`; a.click()
}
async function doExportCsv() {
  const data = viewGraphData.value || store.graphData
  if (!data) { ElMessage.warning('没有可导出的图数据'); return }
  const ts = new Date().toISOString().replace(/[:.]/g, '-')
  const base = store.graphData?.rootTableName || 'graph'
  const nodesCsv = toCsv((data.nodes || []).map(n => {
    const d = n.data || n.attributes || {}
    return {
      id: n.id, name: n.name, nodeType: n.nodeType, layer: n.layer ?? '',
      datasourceName: n.datasourceName || d.datasourceName || '',
      domain: d.domain || n.domain || '', owner: d.owner || n.owner || '',
      tags: (d.tags || n.tags || []).join('|')
    }
  }))
  const linksCsv = toCsv((data.links || []).map(l => ({
    source: l.source, target: l.target,
    sourceName: store.nodeNameMap[l.source] || l.source, targetName: store.nodeNameMap[l.target] || l.target,
    edgeType: l.edgeType || l.relationType || 'FLOWS_TO', taskName: l.taskName || '',
    fieldCount: (l.fieldLinks || []).length
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

// ---------- Write dialogs (shared FormSchema, B2) ----------
const submittingEdge = ref(false)
const assetEdgeVisible = ref(false)
const addEdgeVisible = ref(false)
const editEdgeVisible = ref(false)
const assetEdgeFormRef = ref(null)
const addEdgeFormRef = ref(null)
const editEdgeFormRef = ref(null)
const assetEdgeForm = ref(emptyEdgeForm())
const addEdgeForm = ref(emptyEdgeForm())
const editEdgeForm = ref(emptyEdgeForm())

function enumLabel(f, opt) {
  if (f.key === 'relationType') return EDGE_ENUM_LABELS[opt] || opt
  return opt
}
function freshForm() { return emptyEdgeForm() }

// Per-dialog field subsets (schema-derived).
const assetEdgeFields = computed(() => edgeFormMeta.map(f => (f.key === 'sourceId' || f.key === 'targetId')
  ? { ...f, placeholder: f.key === 'sourceId' ? '搜索源节点' : '搜索目标节点' }
  : f))
const addEdgeFields = computed(() => edgeFormMeta.map(f => (f.key === 'sourceId' ? { ...f, disabled: true } : f)))
const editEdgeFields = computed(() => edgeFormMeta.filter(f => !['sourceId', 'targetId'].includes(f.key)))

// assetSearch map entries so the renderer finds an empty array on first render.
searchResultsMap.sourceId = []
searchResultsMap.targetId = []

function openAssetEdgeDialog() {
  if (!canWrite.value) { ElMessage.warning(t('lineage.perm.writeRequired')); return }
  assetEdgeForm.value = freshForm()
  searchResultsMap.sourceId = []
  searchResultsMap.targetId = []
  assetEdgeVisible.value = true
}
function openAddEdgeDialog() {
  if (!canWrite.value) { ElMessage.warning(t('lineage.perm.writeRequired')); return }
  const cur = store.selectedNode?.id || store.rootNodeId || ''
  addEdgeForm.value = { ...freshForm(), sourceId: cur }
  searchResultsMap.targetId = []
  addEdgeVisible.value = true
}

async function validate(formRef, form) {
  setEdgeForm(form)
  if (!formRef.value) return true
  try {
    await formRef.value.validate()
    return true
  } catch (e) {
    return false
  }
}

async function submitAssetEdge() {
  if (!(await validate(assetEdgeFormRef, assetEdgeForm.value))) return
  const f = assetEdgeForm.value
  if (!f.sourceId || !f.targetId) { ElMessage.warning('请选择源节点与目标节点'); return }
  submittingEdge.value = true
  try {
    await lineageApi.saveEdge(f.sourceId, f.targetId, edgeAttrsFrom(f))
    ElMessage.success('血缘关系已创建')
    announce('血缘关系已创建')
    assetEdgeVisible.value = false
    if (store.rootNodeId) await store.loadGraph()
    else await store.refocus(f.targetId)
  } catch (e) { ElMessage.error('保存失败：' + (e.message || '未知错误')) } finally { submittingEdge.value = false }
}

async function submitAddEdge() {
  if (!(await validate(addEdgeFormRef, addEdgeForm.value))) return
  const f = addEdgeForm.value
  if (!f.sourceId || !f.targetId) { ElMessage.warning('请选择目标节点'); return }
  submittingEdge.value = true
  try {
    await lineageApi.saveEdge(f.sourceId, f.targetId, edgeAttrsFrom(f))
    ElMessage.success('血缘关系已创建')
    announce('血缘关系已创建')
    addEdgeVisible.value = false
    if (store.rootNodeId) await store.loadGraph()
    else await store.refocus(f.targetId)
  } catch (e) { ElMessage.error('保存失败：' + (e.message || '未知错误')) } finally { submittingEdge.value = false }
}

async function submitEditEdge() {
  if (!(await validate(editEdgeFormRef, editEdgeForm.value))) return
  const f = editEdgeForm.value
  if (!f.sourceId || !f.targetId) { ElMessage.warning('无效的边端点'); return }
  submittingEdge.value = true
  try {
    await lineageApi.patchEdge(f.sourceId, f.targetId, edgeAttrsFrom(f))
    ElMessage.success('已更新血缘关系')
    announce('已更新血缘关系')
    editEdgeVisible.value = false
    await store.loadGraph()
  } catch (e) { ElMessage.error('更新失败：' + (e.message || '未知错误')) } finally { submittingEdge.value = false }
}

// ---------- Edge edit / delete (from canvas) ----------
function onEdgeEdit(edgeData) {
  if (!canWrite.value) { ElMessage.warning(t('lineage.perm.writeRequired')); return }
  if (!edgeData?.source || !edgeData?.target) { ElMessage.warning('无效的边数据'); return }
  editEdgeForm.value = {
    ...freshForm(),
    sourceId: edgeData.source,
    targetId: edgeData.target,
    relationType: edgeData?.relationType || edgeData?.edgeType || 'FLOWS_TO',
    taskName: edgeData?.taskName || '',
    sql: edgeData?.sql || '',
    description: edgeData?.description || ''
  }
  editEdgeVisible.value = true
}
async function onEdgeDelete(edgeData) {
  if (!canWrite.value) { ElMessage.warning(t('lineage.perm.writeRequired')); return }
  if (!edgeData?.source || !edgeData?.target) { ElMessage.warning('无效的边数据'); return }
  const sn = store.nodeNameMap[edgeData.source] || edgeData.source
  const tn = store.nodeNameMap[edgeData.target] || edgeData.target
  try { await ElMessageBox.confirm(`确认删除 ${sn} → ${tn} 之间的血缘关系？`, '删除血缘边', { type: 'warning' }) } catch (e) { return }
  try {
    await lineageApi.deleteEdge(edgeData.source, edgeData.target)
    ElMessage.success('已删除血缘边')
    announce('已删除血缘边')
    await store.loadGraph()
  } catch (e) { ElMessage.error('删除失败：' + (e.message || '未知错误')) }
}

async function removeNeighborLink(nb) {
  if (!canWrite.value) { ElMessage.warning(t('lineage.perm.writeRequired')); return }
  const curId = store.selectedNode?.id
  if (!curId || !nb?.id) return
  const fromId = nb.dir === 'down' ? curId : nb.id
  const toId = nb.dir === 'down' ? nb.id : curId
  const nm = nb.name || store.nodeNameMap[nb.id] || nb.id
  try { await ElMessageBox.confirm(`确认移除与「${nm}」的血缘关联？`, '删除关联', { type: 'warning' }) } catch (e) { return }
  try {
    await lineageApi.deleteEdge(fromId, toId)
    ElMessage.success('已移除关联')
    announce('已移除关联')
    await store.loadGraph()
  } catch (e) { ElMessage.error('移除失败：' + (e.message || '未知错误')) }
}

// ---------- WebSocket ----------
let ws = null
function connectWs() {
  try {
    const token = localStorage.getItem('td_token')
    if (!token) { store.setWsConnected(false); return }
    const base = import.meta.env.DEV ? 'ws://localhost:8080' : `ws://${location.host}`
    ws = new WebSocket(`${base}/ws/lineage?token=${token}`)
    ws.onopen = () => store.setWsConnected(true)
    ws.onclose = () => { store.setWsConnected(false); setTimeout(connectWs, 5000) }
    ws.onerror = () => store.setWsConnected(false)
  } catch (e) { store.setWsConnected(false) }
}

// ---------- Watchers (mirror prior behaviour) ----------
watch(() => [store.graphLevel, store.graphDirection, store.graphDepth, store.nodeTypeFilter], () => {
  if (store.rootNodeId && !store.graphLoading) store.loadGraph()
})
watch(() => store.layoutMode, () => { store.clearOverrides(); store.clearColumnHighlight() })

const renderStart = ref(0)
watch(() => store.graphLoading, (loading) => {
  if (loading) { renderStart.value = performance.now() }
  else if (renderStart.value) {
    const ms = Math.round(performance.now() - renderStart.value)
    store.setRenderStats({
      nodes: store.graphData?.nodes?.length || 0,
      edges: store.graphData?.links?.length || 0,
      ms
    })
    renderStart.value = 0
  }
})

// ---------- aria-live announce ----------
function announce(msg) {
  if (ariaRef.value) ariaRef.value.textContent = msg
}

// ---------- Lifecycle ----------
let keyboardUninstall = null
onMounted(() => {
  store.loadRecent()
  store.loadBookmarks()
  try {
    const raw = localStorage.getItem('td_lineage_tree_collapsed')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) store.treeCollapsedNodes = parsed
    }
  } catch (e) { /* ignore storage failures */ }
  if (store.layers.dq && !store.dqLoaded) store.loadDqMap(false)
  connectWs()
  // Install the 6 global shortcuts (B4).
  keyboardUninstall = useLineageKeyboard({
    rootSearchEl: rootSearchRef,
    canvasEl: canvasRef,
    onOpenNewEdge: openAssetEdgeDialog,
    onExportPng: doExportPng,
    notify: { success: (m) => ElMessage.success(m), warning: (m) => ElMessage.warning(m) }
  })
  if (store.rootNodeId && !store.graphData && !store.isOverview) {
    store.loadGraph().catch(() => store.loadOverview())
  }
})

onBeforeUnmount(() => {
  keyboardUninstall?.()
  ws?.close()
})
</script>

<style scoped>
/* ---------- Design tokens (03 §3.1 → CSS vars, 06 §6.4). Light theme default. ----------
 * Dark mode is intentionally NOT implemented in this phase; wire it later by adding a
 * sibling `[data-theme="dark"]` override that remaps the same --lg-* variables, e.g.:
 *   [data-theme="dark"] .lineage-v2-page {
 *     --lg-bg-canvas:#0B1220; --lg-surface:#111827; --lg-text:#E2E8F0; --lg-text-sub:#94A3B8;
 *   }
 */
.lineage-v2-page {
  --lg-bg-canvas: #F5F7FA;
  --lg-surface: #FFFFFF;
  --lg-text: #0F172A;
  --lg-text-sub: #475569;
  --lg-text-muted: #64748B;
  --lg-border: #E2E8F0;
  --lg-accent-root: #FBBF24;
  --lg-accent-up: #10B981;
  --lg-accent-down: #38BDF8;
  --lg-accent-agg: #64748B;
  --lg-accent-hl: #8B5CF6;
  --lg-gap: 12px;
  --lg-tb-h: 56px;
  --lg-sb-h: 28px;
  --lg-fs-xs: 11px;
  --lg-fs-sm: 12px;
  --lg-fs-md: 14px;
  --lg-fs-lg: 16px;
  --lg-fs-xl: 20px;
  display: flex;
  gap: var(--lg-gap);
  height: 100%;
  min-height: 0;
}

/* Screen-reader-only text (06 §6.1 sr-only style). */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* [B] Asset tree */
.lv2-tree {
  width: 260px;
  min-width: 260px;
  flex-shrink: 0;
  background: var(--lg-surface, #fff);
  border: 1px solid var(--lg-border, #E2E8F0);
  border-radius: 8px;
  overflow: hidden;
}

/* Main column */
.lv2-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--lg-gap);
}

/* [A] Toolbar */
.lv2-toolbar {
  display: flex;
  align-items: center;
  gap: 6px;
  height: var(--lg-tb-h, 56px);
  background: var(--lg-surface, #fff);
  border: 1px solid var(--lg-border, #E2E8F0);
  border-radius: 8px;
  padding: 0 10px;
  overflow-x: auto;
  white-space: nowrap;
}
.lv2-toolbar section {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.tb-sep { height: 20px; margin: 0 2px; }
.tb-search { width: 170px; }
.tb-f { width: 92px; }
.tb-d { width: 90px; }
.tb-label { font-size: var(--lg-fs-sm, 12px); color: var(--lg-text-muted, #64748B); }
.so-name { margin-right: 8px; }

/* [C]+[D] body */
.lv2-body {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: var(--lg-gap);
}
.lv2-canvas {
  flex: 1;
  min-width: 0;
  position: relative;
  background: var(--lg-bg-canvas, #F5F7FA);
  border-radius: 8px;
  overflow: hidden;
}
.lv2-graph-focus {
  width: 100%;
  height: 100%;
  outline: none;
}
.lv2-graph-focus:focus-visible {
  outline: 2px solid var(--lg-accent-hl, #8B5CF6);
  outline-offset: -2px;
}
.lv2-graph-stats {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: #37474f;
  font-size: 13px;
  pointer-events: none;
}

/* [D] Detail panel — 3 sections */
.lv2-detail {
  width: 380px;
  min-width: 380px;
  flex-shrink: 0;
  background: var(--lg-surface, #fff);
  border: 1px solid var(--lg-border, #E2E8F0);
  border-radius: 8px;
  padding: 14px 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.lv2-detail-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.lv2-detail-chip {
  flex-shrink: 0; width: 20px; height: 20px; border-radius: 5px;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 9px; font-weight: 800; line-height: 1;
}
.lv2-detail-name { font-weight: 700; font-size: 15px; color: var(--lg-text, #0F172A); word-break: break-all; flex: 1; }
.lv2-detail-meta { display: flex; flex-direction: column; gap: 4px; font-size: var(--lg-fs-sm, 12px); color: var(--lg-text-sub, #475569); }
.lv2-detail-cell { color: var(--lg-text-muted, #64748B); }
.lv2-tag { transform: scale(0.88); margin-right: 2px; }
.lv2-tag-more { color: var(--lg-text-muted, #64748B); font-size: 11px; }

.lv2-gov-toggle {
  border: none;
  background: transparent;
  text-align: left;
  padding: 4px 0;
  font-size: 12px;
  color: var(--lg-accent-hl, #8B5CF6);
  cursor: pointer;
}
.lv2-gov {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 10px;
  border: 1px solid var(--lg-border, #E2E8F0);
  border-radius: 6px;
  background: #F8FAFC;
}
.lv2-gov-row { display: flex; align-items: center; gap: 8px; font-size: 12px; min-width: 0; }
.lv2-gov-k { flex-shrink: 0; width: 68px; color: var(--lg-text-muted, #64748B); }
.lv2-gov-v { color: var(--lg-text, #0F172A); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.lv2-detail-sec { border-top: 1px solid #eef2f6; padding-top: 10px; display: flex; flex-direction: column; gap: 8px; }
.lv2-detail-sec-title { font-size: 13px; font-weight: 600; color: var(--lg-text-sub, #475569); }
.lv2-neighbors { display: flex; flex-direction: column; gap: 6px; max-height: 220px; overflow-y: auto; }
.lv2-empty { padding: 8px 4px; font-size: var(--lg-fs-sm, 12px); color: #90a4ae; }
.lv2-empty-detail { color: #94A3B8; font-size: var(--lg-fs-sm, 12px); text-align: center; padding-top: 40px; }
.lv2-detail-actions { display: flex; gap: 8px; margin-top: 2px; flex-wrap: wrap; align-items: center; }
.lv2-detail-actions > * { flex-shrink: 0; }

/* Statusbar */
.lv2-status {
  display: flex;
  align-items: center;
  gap: 12px;
  height: var(--lg-sb-h, 28px);
  padding: 0 12px;
  background: var(--lg-surface, #fff);
  border: 1px solid var(--lg-border, #E2E8F0);
  border-radius: 6px;
  font-size: var(--lg-fs-xs, 11px);
  color: var(--lg-text-muted, #64748B);
}
.st-item { display: inline-flex; align-items: center; gap: 4px; color: var(--lg-text-sub, #475569); }
.st-item .el-tag { transform: scale(0.9); }
.st-spacer { flex: 1; }
.st-chip { padding: 1px 8px; border-radius: 10px; font-size: 10px; font-weight: 700; letter-spacing: 0.5px; }
.st-read { background: #F1F5F9; color: #475569; }
.st-write { background: #DCFCE7; color: #15803D; }

/* Dialogs shared */
.dlg-body { display: flex; flex-direction: column; gap: 12px; }
.dlg-row { display: flex; align-items: center; gap: 12px; }
.dlg-row .el-button { flex-shrink: 0; }
.dlg-label { flex-shrink: 0; font-size: 12px; color: #6b7280; width: 34px; }
.dlg-result { display: flex; flex-direction: column; gap: 8px; border-top: 1px solid #e5e7eb; padding-top: 12px; }
.dlg-chain { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; font-size: 12px; }
.dlg-arrow { color: #9ca3af; margin: 0 6px; }
.dlg-table { margin-top: 8px; }
.lg-dlg-hint { color: var(--lg-text-muted, #64748B); font-size: 12px; margin: 0 0 10px; }

/* Narrow-viewport collapse rules */
@media (max-width: 996px) {
  .lv2-tree { width: 48px; min-width: 48px; }
}
@media (max-width: 768px) {
  .lv2-detail {
    position: fixed;
    right: 0;
    top: 54px;
    bottom: 40px;
    width: 380px;
    max-width: 88vw;
    z-index: 100;
    box-shadow: -4px 0 24px rgba(15, 23, 42, 0.12);
  }
}
</style>