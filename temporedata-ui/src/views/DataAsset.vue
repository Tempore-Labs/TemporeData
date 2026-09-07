<template>
  <div class="data-asset-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">数据目录</h2>
        <div class="page-desc">表资产浏览 · 字段详情 · 数据标签 · 血缘关系</div>
      </div>
      <el-button :icon="Refresh" @click="handleSync" :loading="syncing">同步元数据</el-button>
    </div>

    <el-card shadow="never" class="main-card">
      <div class="asset-layout">
        <!-- Left: catalog tree -->
        <div class="asset-tree-panel">
          <div class="panel-title">数据目录</div>
          <el-input
            v-model="treeFilter"
            placeholder="搜索数据库/表..."
            clearable
            size="small"
            :prefix-icon="Search"
            class="tree-search"
            @input="filterTree"
          />
          <el-tree
            ref="treeRef"
            :data="treeData"
            :props="treeProps"
            node-key="id"
            highlight-current
            default-expand-all
            :expand-on-click-node="true"
            @node-click="handleNodeClick"
            v-loading="treeLoading"
            style="flex: 1; overflow-y: auto;"
          >
            <template #default="{ data }">
              <span class="tree-node">
                <el-icon v-if="data.type === 'DATASOURCE'" class="node-icon ds-icon"><Coin /></el-icon>
                <el-icon v-else-if="data.type === 'SCHEMA'" class="node-icon schema-icon"><Folder /></el-icon>
                <el-icon v-else class="node-icon tbl-icon"><Document /></el-icon>
                <span class="tree-label">{{ data.label }}</span>
                <span v-if="data.type === 'TABLE' && data.rowCount != null" class="tree-count">{{ fmt(data.rowCount) }}</span>
              </span>
            </template>
          </el-tree>
        </div>

        <!-- Right: detail -->
        <div class="asset-detail-panel">
          <el-empty v-if="!selectedTable" description="请从左侧选择一张表查看详情" />
          <template v-else v-loading="detailLoading">
            <div class="panel-title-row">
              <span class="panel-title">{{ selectedTable.tableName }}</span>
              <div class="detail-actions"></div>
            </div>

            <el-tabs v-model="activeTab">
              <!-- Tab 1: Table detail -->
              <el-tab-pane label="表详情" name="detail">
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="表名">{{ selectedTable.tableName || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="Schema">{{ selectedTable.schemaName || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="数据源">{{ selectedTable.datasourceName || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="行数">{{ fmt(selectedTable.rowCount ?? 0) }}</el-descriptions-item>
                  <el-descriptions-item label="同步时间">{{ selectedTable.syncTime || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="数据分级">
                    <el-select v-model="selectedTable.dataLevelId" size="small" clearable placeholder="选择分级" style="width: 180px" @change="saveTableGovernance">
                      <el-option v-for="l in levels" :key="l.id" :label="l.name + ' (' + l.code + ')'" :value="l.id" />
                    </el-select>
                  </el-descriptions-item>
                  <el-descriptions-item label="数据分类">
                    <el-select v-model="selectedTable.dataCategoryId" size="small" clearable placeholder="选择分类" style="width: 180px" @change="saveTableGovernance">
                      <el-option v-for="c in categoryOptions" :key="c.id" :label="c.label" :value="c.id" />
                    </el-select>
                  </el-descriptions-item>
                  <el-descriptions-item label="备注">
                    <div class="comment-cell">
                      <el-input
                        v-if="editingComment"
                        v-model="commentValue"
                        size="small"
                        style="width: 240px"
                        @blur="saveTableComment"
                        @keydown.enter="saveTableComment"
                        @keydown.escape="cancelCommentEdit"
                      />
                      <template v-else>
                        <span class="comment-text" :class="{ placeholder: !selectedTable.comment }" @click="startCommentEdit">
                          {{ selectedTable.comment || '点击编辑表备注' }}
                        </span>
                        <el-icon class="edit-icon" @click="startCommentEdit"><Edit /></el-icon>
                      </template>
                    </div>
                  </el-descriptions-item>
                </el-descriptions>

                <div class="sub-title">字段列表（{{ (selectedTable.columns || []).length }}）</div>
                <el-table :data="selectedTable.columns" stripe size="small" style="width: 100%">
                  <el-table-column prop="columnName" label="字段名" width="170" show-overflow-tooltip />
                  <el-table-column prop="dataType" label="类型" width="130">
                    <template #default="{ row }">
                      <el-tag size="small" type="info">{{ row.dataType }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="长度" width="80" align="center">
                    <template #default="{ row }">
                      <span v-if="row.columnSize != null">{{ row.columnSize }}<span v-if="row.decimalDigits != null">,{{ row.decimalDigits }}</span></span>
                      <span v-else>-</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="默认值" width="110" show-overflow-tooltip>
                    <template #default="{ row }">
                      <span v-if="row.defaultValue != null && row.defaultValue !== ''" class="mono">{{ row.defaultValue }}</span>
                      <span v-else>-</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="isNullable" label="可空" width="70" align="center">
                    <template #default="{ row }">
                      <el-tag :type="row.isNullable ? 'warning' : 'success'" size="small">{{ row.isNullable ? 'YES' : 'NO' }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="isPrimaryKey" label="主键" width="70" align="center">
                    <template #default="{ row }">
                      <el-tag v-if="row.isPrimaryKey" type="danger" size="small">PK</el-tag>
                      <span v-else>-</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="字段分级" width="130">
                    <template #default="{ row }">
                      <el-select v-model="row.dataLevelId" size="small" clearable placeholder="分级" @change="saveColumnGovernance(row)">
                        <el-option v-for="l in levels" :key="l.id" :label="l.name" :value="l.id" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="标签" width="70" align="center">
                    <template #default="{ row }">
                      <el-button size="small" link type="primary" :icon="PriceTag" @click="openColTags(row)">标签</el-button>
                    </template>
                  </el-table-column>
                  <el-table-column label="备注" min-width="170">
                    <template #default="{ row }">
                      <div class="comment-cell" @click="startEdit(row)">
                        <template v-if="editingId === row.id">
                          <el-input
                            ref="editInputRef"
                            v-model="editValue"
                            size="small"
                            @blur="saveEdit(row)"
                            @keydown.enter="saveEdit(row)"
                            @keydown.escape="cancelEdit"
                          />
                        </template>
                        <template v-else>
                          <span class="comment-text" :class="{ placeholder: !row.comment }">{{ row.comment || '点击编辑备注' }}</span>
                          <el-icon class="edit-icon"><Edit /></el-icon>
                        </template>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <!-- Tab 2: Tags -->
              <el-tab-pane label="数据标签" name="tags">
                <div class="tags-toolbar">
                  <AssetTagSelect v-model="selectedTagIds" placeholder="选择标签" class="asset-tag-select" />
                  <el-button type="primary" :icon="PriceTag" :loading="savingTags" @click="saveTags">保存打标</el-button>
                </div>
                <div class="sub-title">当前资产标签</div>
                <div v-if="tableTags.length" class="asset-tags-box">
                  <el-tag
                    v-for="b in tableTags"
                    :key="b.tagId"
                    closable
                    :color="b.tagColor || '#409EFF'"
                    style="border:none;color:#fff"
                    @close="handleUnbind(b)"
                  >
                    {{ b.tagName || b.tagId }}
                  </el-tag>
                </div>
                <el-empty v-else description="该表暂无标签" :image-size="60" />
              </el-tab-pane>

              <!-- Tab 3: Lineage -->
              <el-tab-pane label="血缘关系" name="lineage">
                <div class="lineage-section" v-loading="lineageLoading">
                  <div class="lineage-col">
                    <div class="lineage-col-title">上游来源</div>
                    <div v-if="lineageData && lineageData.upstream && lineageData.upstream.length">
                      <div v-for="(n, i) in lineageData.upstream" :key="i" class="lineage-node">
                        <el-tag size="small" :type="relationTag(n.relationType)">{{ n.relationType }}</el-tag>
                        <span>{{ n.tableName }}</span>
                        <span class="lineage-rel">{{ n.relationName }}</span>
                      </div>
                    </div>
                    <div v-else class="lineage-empty">无上游依赖</div>
                  </div>
                  <div class="lineage-col">
                    <div class="lineage-col-title">下游消费</div>
                    <div v-if="lineageData && lineageData.downstream && lineageData.downstream.length">
                      <div v-for="(n, i) in lineageData.downstream" :key="i" class="lineage-node">
                        <el-tag size="small" :type="relationTag(n.relationType)">{{ n.relationType }}</el-tag>
                        <span>{{ n.tableName }}</span>
                        <span class="lineage-rel">{{ n.relationName }}</span>
                      </div>
                    </div>
                    <div v-else class="lineage-empty">无下游消费</div>
                  </div>
                </div>
                <el-button size="small" :icon="Share" class="lineage-go" @click="goLineage">进入血缘分析（完整图谱）</el-button>
              </el-tab-pane>
              <!-- Tab 4: Quality audit -->
              <el-tab-pane label="质量稽核" name="audit">
                <div class="audit-section" v-loading="auditLoading">
                  <div class="audit-header">
                    <el-button size="small" type="primary" :icon="Search" :loading="auditLoading" @click="runAudit">
                      执行稽核
                    </el-button>
                    <span v-if="auditReport" class="audit-summary">
                      规则 {{ auditReport.totalRules }} 项 ·
                      <el-tag size="small" type="success">通过 {{ auditReport.passCount }}</el-tag>
                      <el-tag size="small" type="danger">失败 {{ auditReport.failCount }}</el-tag>
                      <el-tag size="small" type="info">通过率 {{ auditReport.passRate || '-' }}</el-tag>
                      <span class="audit-time">报告时间：{{ auditReport.reportTime || '-' }}</span>
                    </span>
                  </div>
                  <el-table v-if="auditReport && auditReport.ruleResults && auditReport.ruleResults.length" :data="auditReport.ruleResults" size="small" stripe style="width: 100%">
                    <el-table-column label="规则名称" prop="ruleName" min-width="140" />
                    <el-table-column label="类型" width="110">
                      <template #default="{ row }">
                        <el-tag size="small">{{ row.ruleType }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="状态" width="90">
                      <template #default="{ row }">
                        <el-tag size="small" :type="auditStatusTag(row.status)">{{ row.status }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="结果" prop="result" min-width="160" show-overflow-tooltip />
                    <el-table-column label="检查时间" prop="checkTime" width="170" />
                  </el-table>
                  <el-empty v-else description="该表暂无质量规则，请先在「质量规则」模块中配置" />
                </div>
              </el-tab-pane>
            </el-tabs>
          </template>
        </div>
      </div>
    </el-card>

    <!-- Column tags dialog -->
    <el-dialog v-model="colTagVisible" :title="'字段标签 - ' + (colTagTarget?.columnName || '')" width="480px" destroy-on-close>
      <AssetTagSelect v-model="colTagIds" placeholder="选择字段标签" />
      <div class="sub-title">当前字段标签</div>
      <div v-if="colTags.length" class="asset-tags-box">
        <el-tag v-for="b in colTags" :key="b.tagId" closable :color="b.tagColor || '#409EFF'" style="border:none;color:#fff" @close="handleColUnbind(b)">
          {{ b.tagName || b.tagId }}
        </el-tag>
      </div>
      <el-empty v-else description="该字段暂无标签" :image-size="50" />
      <template #footer>
        <el-button @click="colTagVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingColTags" @click="saveColTags">保存打标</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Search, Share, Edit, Folder, Document, Coin, PriceTag } from '@element-plus/icons-vue'
import { catalogApi } from '@/api/modules/catalog'
import { tagApi } from '@/api/modules/tag'
import { securityApi } from '@/api/modules/security'
import { qualityApi } from '@/api/modules/quality'
import AssetTagSelect from '@/components/AssetTagSelect.vue'

const router = useRouter()

const treeRef = ref(null)
const editInputRef = ref(null)
const treeFilter = ref('')
const treeData = ref([])
const originalTree = ref([])
const treeLoading = ref(false)
const detailLoading = ref(false)
const syncing = ref(false)
const selectedTable = ref(null)
const activeTab = ref('detail')

const tableTags = ref([])
const selectedTagIds = ref([])
const savingTags = ref(false)

// Column-level tags
const colTagVisible = ref(false)
const colTagTarget = ref(null)
const colTagIds = ref([])
const colTags = ref([])
const savingColTags = ref(false)

// Governance (sensitivity level / category)
const levels = ref([])
const categoryOptions = ref([])

const lineageData = ref(null)
const lineageLoading = ref(false)

const auditReport = ref(null)
const auditLoading = ref(false)

async function loadAudit() {
  if (!selectedTable.value || !selectedTable.value.datasourceId || !selectedTable.value.tableName) return
  auditLoading.value = true
  try {
    auditReport.value = await qualityApi.report(selectedTable.value.datasourceId, selectedTable.value.tableName)
  } catch (e) {
    auditReport.value = null
  } finally {
    auditLoading.value = false
  }
}

async function runAudit() {
  if (!selectedTable.value) return
  auditLoading.value = true
  try {
    await qualityApi.batchExecute(selectedTable.value.datasourceId, selectedTable.value.tableName)
    ElMessage.success('稽核执行完成')
    await loadAudit()
  } catch (e) {
    ElMessage.error('稽核执行失败: ' + (e.message || '未知错误'))
  } finally {
    auditLoading.value = false
  }
}

function auditStatusTag(status) {
  if (status === 'PASS') return 'success'
  if (status === 'FAIL') return 'danger'
  return 'info'
}

const editingId = ref(null)
const editValue = ref('')
const editingComment = ref(false)
const commentValue = ref('')

const treeProps = {
  children: 'children',
  label: 'label'
}

function fmt(n) {
  if (n == null) return '-'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

function filterTree() {
  const kw = treeFilter.value.toLowerCase()
  const walk = (nodes) => {
    if (!nodes) return []
    return nodes.reduce((acc, node) => {
      const labelMatch = (node.label || '').toLowerCase().includes(kw)
      const children = walk(node.children)
      if (labelMatch || children.length) {
        acc.push({ ...node, children: labelMatch ? node.children : children })
      }
      return acc
    }, [])
  }
  treeData.value = kw ? walk(originalTree.value) : originalTree.value
}

async function handleNodeClick(data) {
  if (data.type !== 'TABLE' || !data.id) return
  detailLoading.value = true
  activeTab.value = 'detail'
  try {
    selectedTable.value = await catalogApi.detail(data.id) || null
    if (selectedTable.value) {
      await Promise.all([loadTags(), loadLineage(), loadAudit()])
    }
  } catch (e) {
    ElMessage.error('加载表详情失败: ' + (e.message || '未知错误'))
  } finally {
    detailLoading.value = false
  }
}

async function loadTree() {
  treeLoading.value = true
  try {
    const result = await catalogApi.tree()
    treeData.value = Array.isArray(result) ? result : []
    originalTree.value = treeData.value
  } catch {
    treeData.value = []
    originalTree.value = []
  } finally {
    treeLoading.value = false
  }
}

async function handleSync() {
  syncing.value = true
  try {
    const res = await catalogApi.syncAll()
    ElMessage.success(`元数据同步完成: 成功${res.success} / 失败${res.fail}`)
    await loadTree()
    if (selectedTable.value) {
      selectedTable.value = await catalogApi.detail(selectedTable.value.id)
    }
  } catch (e) {
    ElMessage.error('同步失败: ' + (e.message || '未知错误'))
  } finally {
    syncing.value = false
  }
}

// ---- Comment editing ----

function startEdit(row) {
  editingId.value = row.id
  editValue.value = row.comment || ''
  nextTick(() => {
    if (editInputRef.value) editInputRef.value.focus()
  })
}

function cancelEdit() {
  editingId.value = null
  editValue.value = ''
}

async function saveEdit(row) {
  if (editingId.value !== row.id) return
  const newComment = editValue.value.trim()
  editingId.value = null
  editValue.value = ''
  if (newComment === (row.comment || '')) return
  try {
    await catalogApi.updateComment(null, row.id, newComment)
    row.comment = newComment
    ElMessage.success('字段备注已更新')
  } catch (e) {
    ElMessage.error('更新备注失败: ' + (e.message || '未知错误'))
  }
}

function startCommentEdit() {
  editingComment.value = true
  commentValue.value = selectedTable.value.comment || ''
}

function cancelCommentEdit() {
  editingComment.value = false
  commentValue.value = ''
}

async function saveTableComment() {
  if (!editingComment.value) return
  const newComment = commentValue.value.trim()
  editingComment.value = false
  commentValue.value = ''
  if (!selectedTable.value || newComment === (selectedTable.value.comment || '')) return
  try {
    await catalogApi.updateComment(selectedTable.value.id, null, newComment)
    selectedTable.value.comment = newComment
    ElMessage.success('表备注已更新')
  } catch (e) {
    ElMessage.error('更新表备注失败: ' + (e.message || '未知错误'))
  }
}

// ---- Tags ----

async function loadTags() {
  try {
    tableTags.value = await tagApi.bindings('TABLE', selectedTable.value.id) || []
    selectedTagIds.value = tableTags.value.map(b => b.tagId)
  } catch (e) {
    tableTags.value = []
  }
}

async function saveTags() {
  if (!selectedTable.value) return
  savingTags.value = true
  try {
    await tagApi.bind({
      assetType: 'TABLE',
      assetId: selectedTable.value.id,
      assetName: selectedTable.value.tableName || '',
      tagIds: selectedTagIds.value
    })
    ElMessage.success('打标成功')
    await loadTags()
  } catch (e) {
    ElMessage.error('打标失败: ' + (e.message || '未知错误'))
  } finally {
    savingTags.value = false
  }
}

async function handleUnbind(binding) {
  try {
    await tagApi.unbind(binding.tagId, 'TABLE', selectedTable.value.id)
    ElMessage.success('已解绑标签')
    await loadTags()
  } catch (e) {
    ElMessage.error('解绑失败: ' + (e.message || '未知错误'))
  }
}

// ---- Governance ----

function flattenCategories(nodes, depth = 0) {
  const out = []
  for (const n of nodes || []) {
    out.push({ id: n.id, label: '　'.repeat(depth) + n.name })
    out.push(...flattenCategories(n.children, depth + 1))
  }
  return out
}

async function loadGovernanceOptions() {
  try {
    levels.value = await securityApi.listLevels() || []
  } catch (e) { levels.value = [] }
  try {
    const cats = await securityApi.categoryTree() || []
    categoryOptions.value = flattenCategories(cats)
  } catch (e) { categoryOptions.value = [] }
}

async function saveTableGovernance() {
  if (!selectedTable.value) return
  try {
    await catalogApi.updateGovernance({
      tableId: selectedTable.value.id,
      dataLevelId: selectedTable.value.dataLevelId || null,
      dataCategoryId: selectedTable.value.dataCategoryId || null
    })
    ElMessage.success('表分级已更新')
  } catch (e) {
    ElMessage.error('更新分级失败: ' + (e.message || '未知错误'))
  }
}

async function saveColumnGovernance(row) {
  try {
    await catalogApi.updateGovernance({ columnId: row.id, dataLevelId: row.dataLevelId || null })
    ElMessage.success('字段分级已更新')
  } catch (e) {
    ElMessage.error('更新字段分级失败: ' + (e.message || '未知错误'))
  }
}

// ---- Column tags ----

async function openColTags(row) {
  colTagTarget.value = row
  colTagIds.value = []
  colTags.value = []
  colTagVisible.value = true
  try {
    colTags.value = await tagApi.bindings('COLUMN', row.id) || []
    colTagIds.value = colTags.value.map(b => b.tagId)
  } catch (e) {
    colTags.value = []
  }
}

async function saveColTags() {
  if (!colTagTarget.value) return
  savingColTags.value = true
  try {
    await tagApi.bind({
      assetType: 'COLUMN',
      assetId: colTagTarget.value.id,
      assetName: colTagTarget.value.columnName || '',
      tagIds: colTagIds.value
    })
    ElMessage.success('字段打标成功')
    colTags.value = await tagApi.bindings('COLUMN', colTagTarget.value.id) || []
    colTagIds.value = colTags.value.map(b => b.tagId)
  } catch (e) {
    ElMessage.error('字段打标失败: ' + (e.message || '未知错误'))
  } finally {
    savingColTags.value = false
  }
}

async function handleColUnbind(binding) {
  try {
    await tagApi.unbind(binding.tagId, 'COLUMN', colTagTarget.value.id)
    ElMessage.success('已解绑标签')
    colTags.value = await tagApi.bindings('COLUMN', colTagTarget.value.id) || []
    colTagIds.value = colTags.value.map(b => b.tagId)
  } catch (e) {
    ElMessage.error('解绑失败: ' + (e.message || '未知错误'))
  }
}

// ---- Lineage ----

async function loadLineage() {
  lineageLoading.value = true
  try {
    lineageData.value = await catalogApi.lineage(selectedTable.value.id)
  } catch (e) {
    lineageData.value = null
  } finally {
    lineageLoading.value = false
  }
}

function goLineage() {
  if (!selectedTable.value) return
  const nodeId = 'T:' + encodeURIComponent((selectedTable.value.tableName || '').toUpperCase())
  router.push({ path: '/lineage', query: { node: nodeId } })
}

function relationTag(type) {
  const map = { SYNC: 'success', INGESTION: 'warning', QUALITY: 'danger', QUERY: 'info' }
  return map[type] || 'info'
}

onMounted(() => {
  loadTree()
  loadGovernanceOptions()
})
</script>

<style scoped>
.data-asset-page {
  padding: 0;
}

.page-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.page-desc {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
}

.main-card {
  padding: 0;
}

.asset-layout {
  display: flex;
  height: calc(100vh - 220px);
  min-height: 500px;
}

.asset-tree-panel {
  width: 300px;
  min-width: 300px;
  border-right: 1px solid #ebeef5;
  padding-right: 16px;
  display: flex;
  flex-direction: column;
}

.asset-detail-panel {
  flex: 1;
  padding-left: 16px;
  overflow-y: auto;
  min-width: 0;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.panel-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-actions {
  display: flex;
  gap: 8px;
}

.tree-search {
  margin-bottom: 10px;
}

.tree-node {
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.tree-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-icon {
  font-size: 15px;
  flex-shrink: 0;
}

.ds-icon { color: #1a6ff5; }
.schema-icon { color: #f59e0b; }
.tbl-icon { color: #10b981; }

.tree-count {
  margin-left: auto;
  font-size: 11px;
  color: #9ca3af;
}

.sub-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 16px 0 10px;
}

.comment-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  min-height: 24px;
  width: 100%;
}

.comment-text {
  flex: 1;
  font-size: 13px;
}

.comment-text.placeholder {
  color: #c0c4cc;
  font-style: italic;
}

.edit-icon {
  font-size: 14px;
  color: #9ca3af;
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
  cursor: pointer;
}

.comment-cell:hover .edit-icon {
  opacity: 1;
}

.comment-cell:hover .comment-text {
  color: #1a6ff5;
}

.tags-toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}

.asset-tag-select {
  max-width: 420px;
}

.asset-tags-box {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.lineage-section {
  display: flex;
  gap: 24px;
  min-height: 120px;
}

.audit-section {
  min-height: 120px;
}

.audit-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.audit-summary {
  font-size: 13px;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 6px;
}

.audit-time {
  color: #9ca3af;
  font-size: 12px;
}

.lineage-col {
  flex: 1;
}

.lineage-col-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 10px;
}

.lineage-node {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  padding: 6px 0;
  border-bottom: 1px dashed #f0f0f0;
}

.lineage-rel {
  margin-left: auto;
  font-size: 12px;
  color: #9ca3af;
}

.lineage-empty {
  font-size: 12px;
  color: #c0c4cc;
  padding: 8px 0;
}

.lineage-go {
  margin-top: 16px;
}

.mono {
  font-family: Menlo, Consolas, monospace;
  font-size: 12px;
  color: #374151;
}
</style>