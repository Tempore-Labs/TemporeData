<template>
  <div class="catalog">
    <PageHeader title="数据目录">
      <template #actions>
        <el-input v-model="kw" placeholder="搜索表名..." size="small" clearable style="width: 200px" @input="onSearch" />
        <el-button size="small" type="primary" @click="openCreate">+ 新建数据集</el-button>
      </template>
    </PageHeader>

    <div class="catalog-body">
      <!-- Tree -->
      <div class="tree-pane">
        <div class="tree-head">
          <span>数据集</span>
          <span class="tree-count">{{ allDatasets.length }}</span>
        </div>
        <div class="tree-scroll">
          <el-skeleton v-if="loading" :rows="6" animated />
          <template v-else>
            <div v-for="layer in layers" :key="layer" class="layer-group">
              <button class="layer-row" @click="toggleLayer(layer)">
                <el-icon :size="10" class="arrow"><ArrowRight v-if="!openLayers.has(layer)" /><ArrowDown v-else /></el-icon>
                <span class="layer-name">{{ layerLabel(layer) }}</span>
                <span class="layer-count">{{ countOf(layer) }}</span>
              </button>
              <div v-if="openLayers.has(layer)" class="dataset-list">
                <button
                  v-for="ds in filteredOf(layer)"
                  :key="ds.id"
                  class="dataset-row"
                  :class="{ active: selected?.id === ds.id }"
                  @click="select(ds)"
                >
                  <span class="mono">{{ ds.name }}</span>
                  <span class="row-meta">{{ fmtNum(ds.id) }}</span>
                </button>
                <div v-if="filteredOf(layer).length === 0" class="layer-empty">无匹配</div>
              </div>
            </div>
          </template>
        </div>
      </div>

      <!-- Detail -->
      <div class="detail-pane">
        <EmptyState v-if="!selected" icon="Box" title="选择左侧数据表查看详情" desc="支持查看表详情、数据标签、血缘关系与质量报告" />
        <template v-else>
          <div class="detail-head">
            <div class="head-row">
              <span class="type-badge">{{ (selected.layer || 'dataset').toUpperCase() }}</span>
              <span class="mono head-name">{{ selected.name }}</span>
            </div>
            <div class="head-meta">
              <span>代码: <span class="mono">{{ selected.code }}</span></span>
              <span>Owner: {{ selected.owner || '—' }}</span>
              <span>安全级别: {{ selected.securityLevel || '—' }}</span>
            </div>
          </div>

          <el-tabs v-model="activeTab" class="detail-tabs">
            <!-- 表详情 -->
            <el-tab-pane label="表详情" name="detail">
              <DataTable v-if="columns.length" :data="columns">
                <el-table-column prop="name" label="字段名" min-width="160">
                  <template #default="{ row }"><span class="mono">{{ row.name }}</span></template>
                </el-table-column>
                <el-table-column prop="type" label="类型" min-width="140">
                  <template #default="{ row }"><span class="mono type-color">{{ row.type }}</span></template>
                </el-table-column>
                <el-table-column prop="nullable" label="可空" width="80">
                  <template #default="{ row }">
                    <span class="null-badge" :class="{ no: !row.nullable }">{{ row.nullable ? 'YES' : 'NO' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="comment" label="备注" min-width="200" />
              </DataTable>
              <el-descriptions v-else :column="2" border size="small" class="ds-desc">
                <el-descriptions-item label="数据集名称">{{ selected.name }}</el-descriptions-item>
                <el-descriptions-item label="编码">{{ selected.code }}</el-descriptions-item>
                <el-descriptions-item label="分层">{{ selected.layer || '—' }}</el-descriptions-item>
                <el-descriptions-item label="域ID">{{ selected.domainId ?? '—' }}</el-descriptions-item>
                <el-descriptions-item label="Owner">{{ selected.owner || '—' }}</el-descriptions-item>
                <el-descriptions-item label="安全级别">{{ selected.securityLevel || '—' }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ fmtTime(selected.updatedAt) }}</el-descriptions-item>
              </el-descriptions>
              <div v-if="!columns.length && !selected" class="no-col">暂无可展示字段</div>
            </el-tab-pane>

            <!-- 数据标签 -->
            <el-tab-pane label="数据标签" name="tags">
              <div class="tag-pane">
                <div v-if="catalog" class="tag-block">
                  <div class="tag-title">已有标签</div>
                  <div class="tag-list">
                    <el-tag v-for="t in tagArray" :key="t" size="small" closable @close="removeTag(t)">{{ t }}</el-tag>
                    <el-input v-if="addingTag" v-model="newTag" size="small" style="width: 110px" @keyup.enter="addTag" @blur="addingTag = false" />
                    <el-button v-else size="small" text type="primary" @click="addingTag = true">+ 添加标签</el-button>
                  </div>
                  <div class="tag-meta">
                    <span>业务术语: {{ catalog.businessTerm || '—' }}</span>
                    <span>热度: {{ catalog.popularity ?? '—' }}</span>
                    <span>Owner: {{ catalog.owner || '—' }}</span>
                  </div>
                </div>
                <EmptyState v-else icon="PriceTag" title="暂无资产标签" desc="可在资产模块为该数据集维护标签" />
              </div>
            </el-tab-pane>

            <!-- 血缘关系 -->
            <el-tab-pane label="血缘关系" name="lineage">
              <div class="lineage-pane">
                <div v-if="upstream.length || downstream.length">
                  <div class="dir-title" style="color: #2563eb">上游 ({{ upstream.length }})</div>
                  <div v-for="n in upstream" :key="n.id" class="dir-row">
                    <span class="dir-dot" style="background: #2563eb" />
                    <span class="mono">{{ n.label || n.name }}</span>
                  </div>
                  <div class="dir-title" style="color: #10b981; margin-top: 12px">下游 ({{ downstream.length }})</div>
                  <div v-for="n in downstream" :key="n.id" class="dir-row">
                    <span class="dir-dot" style="background: #10b981" />
                    <span class="mono">{{ n.label || n.name }}</span>
                  </div>
                </div>
                <EmptyState v-else icon="Share" title="暂无血缘关系" desc="该数据集尚未接入血缘图" />
              </div>
            </el-tab-pane>

            <!-- 质量报告 -->
            <el-tab-pane label="质量报告" name="quality">
              <div class="quality-pane">
                <div v-for="r in qualityRules" :key="r.id" class="q-item">
                  <div>
                    <div class="q-name">{{ r.ruleType || r.dimension || '质量规则' }}</div>
                    <div class="q-expr mono">{{ r.expression || '—' }}</div>
                  </div>
                  <div class="q-right">
                    <span class="q-threshold">阈值 {{ r.thresholdScore ?? '—' }}</span>
                    <StatusBadge :status="(r.isBlocking ? 'block' : 'pass')" :text="r.isBlocking ? '拦截' : '通过'" />
                  </div>
                </div>
                <div v-if="qualityRules.length === 0" class="q-empty">该数据集暂无质量规则</div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </div>

    <!-- Create dataset dialog -->
    <el-dialog v-model="createVisible" title="新建数据集" width="480px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" placeholder="如 ods_order_detail" />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="createForm.code" placeholder="唯一编码" />
        </el-form-item>
        <el-form-item label="分层">
          <el-select v-model="createForm.layer" placeholder="选择分层">
            <el-option v-for="l in ['ods', 'dwd', 'ads', 'dim', 'other']" :key="l" :label="l.toUpperCase()" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="安全级别">
          <el-select v-model="createForm.securityLevel" placeholder="选择级别">
            <el-option label="L1 公开" value="L1" />
            <el-option label="L2 内部" value="L2" />
            <el-option label="L3 敏感" value="L3" />
            <el-option label="L4 机密" value="L4" />
          </el-select>
        </el-form-item>
        <el-form-item label="Owner">
          <el-input v-model="createForm.owner" placeholder="负责人" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { datasetApi } from '@/api/dataset'
import { assetApi } from '@/api/asset'
import { qualityApi } from '@/api/quality'
import { lineageApi } from '@/api/lineage'
import type { DatasetEntity } from '@/types/dataset'
import type { AssetCatalogEntity } from '@/types/asset'
import type { QualityRuleEntity } from '@/types/quality'

const queryClient = useQueryClient()
const kw = ref('')
const selected = ref<DatasetEntity | null>(null)
const activeTab = ref('detail')
const openLayers = ref<Set<string>>(new Set())

const createVisible = ref(false)
const creating = ref(false)
const createForm = ref<Partial<DatasetEntity>>({ name: '', code: '', layer: '', securityLevel: '', owner: '' })

const allDatasets = ref<DatasetEntity[]>([])
const loading = ref(false)

async function loadDatasets() {
  loading.value = true
  try {
    const page = await datasetApi.page({ page: 0, size: 500 })
    allDatasets.value = page.content || []
    const layers = new Set(page.content.map((d) => d.layer || 'other'))
    openLayers.value = layers
  } finally {
    loading.value = false
  }
}
loadDatasets()

const layers = computed(() => [...new Set(allDatasets.value.map((d) => d.layer || 'other'))])
const countOf = (l: string) => filteredOf(l).length
const filteredOf = (l: string) =>
  allDatasets.value.filter((d) => (d.layer || 'other') === l && (!kw.value || d.name.toLowerCase().includes(kw.value.toLowerCase())))

function layerLabel(l: string) {
  return l.toUpperCase()
}
function toggleLayer(l: string) {
  const next = new Set(openLayers.value)
  next.has(l) ? next.delete(l) : next.add(l)
  openLayers.value = next
}
function onSearch() {
  // keep layers open for visibility
}

function select(ds: DatasetEntity) {
  selected.value = ds
  activeTab.value = 'detail'
}

/* ---- detail data ---- */
const catalog = ref<AssetCatalogEntity | null>(null)
const qualityRules = ref<QualityRuleEntity[]>([])
const columns = ref<{ name: string; type: string; nullable: boolean; comment: string }[]>([])
const upstream = ref<any[]>([])
const downstream = ref<any[]>([])

async function loadDetail() {
  const ds = selected.value
  if (!ds) return
  catalog.value = null
  qualityRules.value = []
  columns.value = []
  upstream.value = []
  downstream.value = []
  try {
    const [cat, rules, versions] = await Promise.allSettled([
      assetApi.catalogGet(ds.id),
      qualityApi.ruleList().then((list) => list.filter((r) => r.datasetId === ds.id)),
      datasetApi.versions(ds.id),
    ])
    if (cat.status === 'fulfilled') catalog.value = cat.value
    if (rules.status === 'fulfilled') qualityRules.value = rules.value
    if (versions.status === 'fulfilled') {
      const last = versions.value.slice(-1)[0]
      if (last?.schemaJson) {
        try {
          const parsed = JSON.parse(last.schemaJson)
          if (Array.isArray(parsed)) columns.value = parsed
          else if (parsed?.columns) columns.value = parsed.columns
        } catch {
          columns.value = []
        }
      }
    }
    // lineage neighbors
    try {
      const g = await lineageApi.graph(String(ds.id), 1)
      const edges = g.links || g.edges || []
      const nodes = g.nodes || []
      upstream.value = edges.filter((e) => e.target === String(ds.id) || e.target === ds.code).map((e) => nodes.find((n) => n.id === e.source)).filter(Boolean)
      downstream.value = edges.filter((e) => e.source === String(ds.id) || e.source === ds.code).map((e) => nodes.find((n) => n.id === e.target)).filter(Boolean)
    } catch {
      // lineage may not be wired for this dataset
    }
  } catch {
    // ignore detail load errors
  }
}

watch(selected, loadDetail)

const tagArray = computed(() => (catalog.value?.tags ? catalog.value.tags.split(',').map((s) => s.trim()).filter(Boolean) : []))
const addingTag = ref(false)
const newTag = ref('')

async function addTag() {
  const t = newTag.value.trim()
  if (!t || !catalog.value || !selected.value) return
  const tags = [...tagArray.value, t]
  try {
    await assetApi.catalogCreate({ datasetId: selected.value.id, tags: tags.join(','), businessTerm: catalog.value.businessTerm, owner: catalog.value.owner, popularity: catalog.value.popularity })
    await loadDetail()
  } catch {
    /* handled by http layer */
  }
  newTag.value = ''
  addingTag.value = false
}

async function removeTag(t: string) {
  if (!catalog.value || !selected.value) return
  const tags = tagArray.value.filter((x) => x !== t)
  try {
    await assetApi.catalogCreate({ datasetId: selected.value.id, tags: tags.join(','), businessTerm: catalog.value.businessTerm, owner: catalog.value.owner, popularity: catalog.value.popularity })
    await loadDetail()
  } catch {
    /* handled */
  }
}

async function submitCreate() {
  if (!createForm.value.name || !createForm.value.code) {
    ElMessage.warning('名称与编码必填')
    return
  }
  creating.value = true
  try {
    await datasetApi.create(createForm.value)
    ElMessage.success('创建成功')
    createVisible.value = false
    createForm.value = { name: '', code: '', layer: '', securityLevel: '', owner: '' }
    queryClient.invalidateQueries({ queryKey: ['datasets'] })
    await loadDatasets()
  } finally {
    creating.value = false
  }
}

function openCreate() {
  createVisible.value = true
}

function fmtNum(n?: number) {
  return n == null ? '—' : String(n)
}
function fmtTime(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 19) : '—'
}
</script>

<style scoped>
.catalog {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px;
}
.catalog-body {
  flex: 1;
  display: flex;
  background: var(--td-surface);
  border: 1px solid var(--td-border);
  border-radius: var(--td-radius-lg);
  box-shadow: var(--td-card-shadow);
  overflow: hidden;
  min-height: 0;
}
.tree-pane {
  width: 280px;
  border-right: 1px solid var(--td-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.tree-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid var(--td-border-light);
  font-size: 13px;
  font-weight: 600;
  color: var(--td-text-2);
}
.tree-count {
  font-size: 10px;
  color: var(--td-text-4);
}
.tree-scroll {
  flex: 1;
  overflow: auto;
  padding: 8px;
}
.layer-group {
  margin-bottom: 4px;
}
.layer-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 8px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  color: var(--td-text-2);
  text-align: left;
}
.layer-row:hover {
  background: var(--td-border-light);
}
.arrow {
  color: var(--td-text-4);
}
.layer-count {
  margin-left: auto;
  font-size: 10px;
  color: var(--td-text-4);
  font-weight: 400;
}
.dataset-list {
  padding-left: 8px;
}
.dataset-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 7px 10px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;
}
.dataset-row:hover {
  background: var(--td-border-light);
}
.dataset-row.active {
  background: var(--td-primary-soft);
}
.dataset-row .mono {
  font-size: 12px;
  color: var(--td-text-2);
}
.dataset-row.active .mono {
  color: var(--td-primary);
  font-weight: 600;
}
.row-meta {
  font-size: 10px;
  color: var(--td-text-4);
}
.layer-empty {
  padding: 6px 10px;
  font-size: 11px;
  color: var(--td-text-4);
}

.detail-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}
.detail-head {
  padding: 14px 20px 10px;
  border-bottom: 1px solid var(--td-border-light);
  flex-shrink: 0;
}
.head-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.type-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--td-success-soft);
  color: var(--td-success-text);
  font-family: var(--td-font-mono);
}
.head-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--td-text-1);
}
.head-meta {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: var(--td-text-4);
}
.detail-tabs {
  flex: 1;
  overflow: auto;
  padding: 0 20px;
}
.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.tag-pane {
  padding: 16px 0;
}
.tag-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--td-text-2);
  margin-bottom: 10px;
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.tag-meta {
  margin-top: 16px;
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: var(--td-text-3);
}
.lineage-pane {
  padding: 16px 0;
}
.dir-title {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 8px;
}
.dir-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 6px;
  border-radius: 8px;
  background: var(--td-bg);
  font-size: 12px;
}
.dir-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.quality-pane {
  padding: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.q-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border: 1px solid var(--td-border-light);
  border-radius: 10px;
  background: var(--td-bg);
}
.q-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--td-text-1);
}
.q-expr {
  font-size: 10px;
  color: var(--td-text-4);
  margin-top: 2px;
}
.q-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.q-threshold {
  font-size: 11px;
  color: var(--td-text-3);
}
.q-empty,
.no-col {
  padding: 24px;
  text-align: center;
  color: var(--td-text-4);
  font-size: 12px;
}
.null-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--td-border-light);
  color: var(--td-text-3);
  font-family: var(--td-font-mono);
}
.null-badge.no {
  background: var(--td-danger-soft);
  color: var(--td-danger);
}
.type-color {
  color: var(--td-purple);
}
.ds-desc {
  margin-top: 8px;
}
</style>
