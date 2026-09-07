<template>
  <div class="datacenter-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">数据中心</h2>
        <div class="page-desc">平台数据资产目录 · 按分类浏览数据资源</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">新增资产条目</el-button>
    </div>

    <!-- Category stats -->
    <div class="stat-cards">
      <div class="stat-card" v-for="card in statCards" :key="card.key" :class="{ active: currentCategory === card.key }" @click="switchCategory(card.key)">
        <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
          <el-icon :size="20"><component :is="card.icon" /></el-icon>
        </div>
        <div>
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="toolbar">
      <el-input v-model="searchKey" placeholder="搜索资产名称或描述..." clearable style="width: 280px" :prefix-icon="Search" @input="doSearch" />
      <span class="toolbar-tip">共 {{ filteredItems.length }} 条资产</span>
    </div>

    <!-- Asset cards -->
    <div class="asset-grid" v-loading="loading">
      <div class="asset-card" v-for="item in filteredItems" :key="item.id" @click="showDetail(item)">
        <div class="asset-head">
          <span class="asset-name" :title="item.name">{{ item.name }}</span>
          <el-tag size="small" :type="categoryTag(item.category)">{{ categoryLabel(item.category) }}</el-tag>
        </div>
        <div class="asset-desc">{{ item.description || '暂无描述' }}</div>
        <div class="asset-foot">
          <el-tag size="small" effect="plain" :type="item.status === 'ENABLED' || item.status === 'ACTIVE' ? 'success' : 'info'">
            {{ item.status || 'ENABLED' }}
          </el-tag>
          <span class="asset-time">{{ item.createTime ? item.createTime.slice(0, 10) : '-' }}</span>
          <span class="asset-ops" @click.stop>
            <el-button size="small" type="primary" link :icon="Edit" @click="openEdit(item)">编辑</el-button>
            <el-popconfirm title="确定删除该条目？" @confirm="handleDelete(item.id)">
              <template #reference>
                <el-button size="small" type="danger" link :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </span>
        </div>
      </div>
      <el-empty v-if="!loading && filteredItems.length === 0" description="暂无资产条目，点击右上角新增" />
    </div>

    <!-- Detail drawer -->
    <el-drawer v-model="detailVisible" :title="currentItem?.name || '资产详情'" size="420px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="名称">{{ currentItem?.name }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ categoryLabel(currentItem?.category) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="(currentItem?.status === 'ENABLED' || currentItem?.status === 'ACTIVE') ? 'success' : 'info'">{{ currentItem?.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ currentItem?.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据源ID">{{ currentItem?.datasourceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="刷新间隔">{{ currentItem?.refreshInterval ? currentItem.refreshInterval + ' 秒' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentItem?.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="配置">
          <pre class="config-pre">{{ currentItem?.config || '-' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- Add/Edit dialog -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑资产条目' : '新增资产条目'" width="560px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px" label-position="right">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="资产名称，如：订单明细宽表" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="说明该资产内容、用途、负责人" />
        </el-form-item>
        <el-form-item label="关联数据源">
          <el-select v-model="form.datasourceId" placeholder="可选，选择后可在详情中跳转" clearable filterable style="width: 100%">
            <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="刷新间隔(秒)">
          <el-input-number v-model="form.refreshInterval" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="配置">
          <el-input v-model="form.config" type="textarea" :rows="3" placeholder="JSON 配置（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, Search, Coin, Files, Share, Link, Refresh, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { datacenterApi } from '@/api/modules/datacenter'
import { datasourceApi } from '@/api/modules/datasource'

const categories = [
  { label: '概览', value: 'OVERVIEW' },
  { label: '数据库', value: 'DATABASE' },
  { label: '表', value: 'TABLE' },
  { label: 'API', value: 'API' }
]

const loading = ref(false)
const items = ref([])
const overview = ref({})
const searchKey = ref('')
const currentCategory = ref('ALL')

// Detail drawer
const detailVisible = ref(false)
const currentItem = ref(null)

// Form dialog
const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const datasources = ref([])

const statCards = computed(() => [
  { key: 'ALL', label: '全部资产', value: items.value.length, icon: 'Files', bg: '#e8f1fe', color: '#1a6ff5' },
  { key: 'DATABASE', label: '数据库', value: overview.value.datasourceCount ?? 0, icon: 'Coin', bg: '#ecfdf5', color: '#10b981' },
  { key: 'TABLE', label: '数据表', value: overview.value.tableCount ?? 0, icon: 'Histogram', bg: '#fef3c7', color: '#f59e0b' },
  { key: 'WORKFLOW', label: '作业流', value: overview.value.workflowCount ?? 0, icon: 'Share', bg: '#ede9fe', color: '#8b5cf6' },
  { key: 'SYNC', label: '同步任务', value: overview.value.syncTaskCount ?? 0, icon: 'Refresh', bg: '#e0f2fe', color: '#0ea5e9' },
  { key: 'API', label: '数据API', value: overview.value.apiCount ?? 0, icon: 'Link', bg: '#f3e8ff', color: '#a855f7' },
  { key: 'QUALITY', label: '质量规则', value: overview.value.qualityRuleCount ?? 0, icon: 'CircleCheck', bg: '#fce7f3', color: '#ec4899' }
])

const filteredItems = computed(() => {
  let list = items.value
  if (currentCategory.value !== 'ALL' && ['OVERVIEW', 'DATABASE', 'TABLE', 'API'].includes(currentCategory.value)) {
    list = list.filter(i => i.category === currentCategory.value)
  }
  if (searchKey.value) {
    const kw = searchKey.value.toLowerCase()
    list = list.filter(i =>
      i.name?.toLowerCase().includes(kw) ||
      i.description?.toLowerCase().includes(kw)
    )
  }
  return list
})

const defaultForm = () => ({
  name: '',
  category: '',
  description: '',
  config: '',
  datasourceId: '',
  refreshInterval: 0
})

const form = ref(defaultForm())
const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

onMounted(() => {
  fetchList()
  fetchOverview()
  fetchDatasources()
})

async function fetchList() {
  loading.value = true
  try {
    items.value = await datacenterApi.list() || []
  } catch (e) {
    ElMessage.error('获取资产列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchOverview() {
  try { overview.value = await datacenterApi.overview() || {} } catch (e) { /* ignore */ }
}

async function fetchDatasources() {
  try { datasources.value = await datasourceApi.list() || [] } catch (e) { /* ignore */ }
}

function switchCategory(key) {
  currentCategory.value = key
}

function doSearch() { /* computed handles it */ }

function showDetail(item) {
  currentItem.value = item
  detailVisible.value = true
}

function openAdd() {
  isEdit.value = false
  form.value = defaultForm()
  formVisible.value = true
}

function openEdit(item) {
  isEdit.value = true
  currentEditId.value = item.id
  form.value = {
    name: item.name,
    category: item.category,
    description: item.description,
    config: item.config,
    datasourceId: item.datasourceId,
    refreshInterval: item.refreshInterval ?? 0
  }
  formVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await datacenterApi.update(currentEditId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await datacenterApi.create(form.value)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    await fetchList()
    await fetchOverview()
  } catch (e) {
    ElMessage.error(isEdit.value ? '更新失败' : '新增失败')
  } finally {
    submitting.value = false
  }
}

const currentEditId = ref('')

async function handleDelete(id) {
  try {
    await datacenterApi.delete(id)
    ElMessage.success('删除成功')
    await fetchList()
    await fetchOverview()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

function categoryLabel(v) {
  return categories.find(c => c.value === v)?.label || v || '-'
}

function categoryTag(v) {
  const map = { OVERVIEW: '', DATABASE: 'success', TABLE: 'warning', API: 'info' }
  return map[v] || 'info'
}
</script>

<style scoped>
.datacenter-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.page-desc {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 12px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-card.active {
  border-color: #1a6ff5;
  background: #f0f6ff;
}

.stat-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #9ca3af;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-tip {
  font-size: 12px;
  color: #9ca3af;
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
  min-height: 120px;
}

.asset-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.asset-card:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.asset-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.asset-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-desc {
  font-size: 13px;
  color: #6b7280;
  min-height: 38px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.asset-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  border-top: 1px dashed #e5e7eb;
  padding-top: 8px;
}

.asset-time {
  font-size: 12px;
  color: #9ca3af;
}

.asset-ops {
  margin-left: auto;
}

.config-pre {
  margin: 0;
  font-size: 12px;
  background: #f8fafc;
  border-radius: 6px;
  padding: 8px;
  white-space: pre-wrap;
  word-break: break-all;
  color: #374151;
}

@media (max-width: 1400px) {
  .stat-cards { grid-template-columns: repeat(4, 1fr); }
}

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
}
</style>
