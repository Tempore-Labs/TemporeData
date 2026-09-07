<template>
  <div class="tag-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">数据标签</h2>
        <div class="page-desc">标签体系 · 术语定义 · 互斥约束 · 审批流 · 资产打标</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">新增标签</el-button>
    </div>

    <!-- Tag definitions -->
    <div class="tag-section">
      <div class="section-title-row">
        <span class="section-title">标签定义</span>
        <div class="tag-toolbar">
          <el-select v-model="filterClassification" placeholder="按分类筛选" clearable style="width: 160px">
            <el-option v-for="c in classifications" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-button :icon="FolderAdd" @click="openClassificationDialog">新增分类</el-button>
        </div>
      </div>
      <el-table :data="filteredTags" v-loading="tagLoading" stripe>
        <el-table-column label="标签/术语" width="200">
          <template #default="{ row }">
            <div class="name-cell">
              <span class="tag-chip" :style="{ background: (row.color || '#409EFF') + '1a', color: row.color || '#409EFF' }">
                {{ row.name }}
              </span>
              <el-tag v-if="row.isTerm" size="small" type="warning" effect="dark">术语</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="编码" width="110" />
        <el-table-column label="分类" width="110">
          <template #default="{ row }">{{ row.classificationName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="互斥" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isMutuallyExclusive" type="danger" size="small">互斥</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="派生自" width="110">
          <template #default="{ row }">
            <span v-if="row.derivedFromTagId" class="derived-tag">{{ derivedName(row.derivedFromTagId) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="definition" label="释义" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.isTerm && row.definition" class="definition-cell">{{ row.definition }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="150" />
        <el-table-column label="操作" width="210" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT'" size="small" type="success" link :icon="Promotion" @click="handleSubmitTag(row)">提交</el-button>
            <template v-if="row.status === 'IN_REVIEW'">
              <el-button size="small" type="success" link :icon="CircleCheck" @click="handleApprove(row)">通过</el-button>
              <el-button size="small" type="danger" link :icon="CircleClose" @click="handleReject(row)">驳回</el-button>
            </template>
            <el-popconfirm title="确定删除该标签？(关联解除)" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Asset tagging -->
    <div class="tag-section">
      <div class="section-title-row">
        <span class="section-title">资产打标</span>
        <div class="tag-actions">
          <el-select v-model="assetType" placeholder="资产类型" style="width: 130px" @change="resetAsset">
            <el-option label="数据表" value="TABLE" />
            <el-option label="数据指标" value="INDICATOR" />
            <el-option label="数据源" value="DATASOURCE" />
          </el-select>
          <el-select
            v-model="selectedAssetId"
            placeholder="选择资产"
            filterable
            style="width: 240px"
            :disabled="!assetType"
            @change="loadAssetTags"
          >
            <el-option v-for="a in assetOptions" :key="a.id" :label="a.label" :value="a.id" />
          </el-select>
          <template v-if="selectedAssetId">
            <AssetTagSelect v-model="selectedTagIds" placeholder="选择标签" class="asset-tag-select" />
            <el-button type="primary" :icon="PriceTag" :loading="savingTags" @click="saveTags">保存打标</el-button>
          </template>
        </div>
      </div>

      <div v-if="currentAssetTags.length" class="asset-tags-box">
        <div class="asset-tags-title">当前资产标签</div>
        <div class="asset-tags">
          <el-tag
            v-for="b in currentAssetTags"
            :key="b.tagId"
            closable
            :color="b.tagColor || '#409EFF'"
            style="border:none;color:#fff"
            @close="handleUnbind(b)"
          >
            {{ b.tagName || b.tagId }}
          </el-tag>
        </div>
      </div>
      <el-empty v-else-if="selectedAssetId && !savingTags" description="该资产暂无标签" :image-size="60" />
    </div>

    <!-- Add/Edit tag dialog -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑标签' : '新增标签'" width="560px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" label-position="right">
        <el-form-item label="类型">
          <el-radio-group v-model="form.isTerm">
            <el-radio :value="false">普通标签</el-radio>
            <el-radio :value="true">术语（含定义/同义词）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：核心业务 / PII敏感" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="唯一编码，如 core / pii" />
        </el-form-item>
        <el-form-item v-if="form.isTerm" label="定义" prop="definition">
          <el-input v-model="form.definition" type="textarea" :rows="3" placeholder="权威定义：该术语的业务含义与口径" />
        </el-form-item>
        <el-form-item v-if="form.isTerm" label="同义词">
          <el-input v-model="form.synonyms" placeholder="逗号分隔，如：销售额,GMV" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.classificationId" clearable filterable placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in classifications" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="颜色">
          <div class="color-picker">
            <el-color-picker v-model="form.color" />
            <el-tag :color="form.color" style="border:none;color:#fff;margin-left:12px">{{ form.name || '标签' }}</el-tag>
          </div>
        </el-form-item>
        <el-form-item label="互斥">
          <el-switch v-model="form.isMutuallyExclusive" active-text="同分类下仅可选一个" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="APPROVED">已发布</el-radio>
            <el-radio value="DRAFT">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="派生自">
          <el-select v-model="form.derivedFromTagId" clearable filterable placeholder="绑定父标签时自动挂载" style="width: 100%">
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" placeholder="标签用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Classification dialog -->
    <el-dialog v-model="classifyVisible" title="新增分类" width="460px" destroy-on-close>
      <el-form ref="classifyFormRef" :model="classifyForm" :rules="classifyRules" label-width="80px" label-position="right">
        <el-form-item label="名称" prop="name"><el-input v-model="classifyForm.name" placeholder="如：数据分级 / 业务域" /></el-form-item>
        <el-form-item label="编码" prop="code"><el-input v-model="classifyForm.code" placeholder="如：level / domain" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="classifyForm.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classifyVisible = false">取消</el-button>
        <el-button type="primary" :loading="classifySaving" @click="handleSaveClassification">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, PriceTag, FolderAdd, Promotion, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { tagApi } from '@/api/modules/tag'
import { catalogApi } from '@/api/modules/catalog'
import { indicatorApi } from '@/api/modules/indicator'
import { datasourceApi } from '@/api/modules/datasource'
import AssetTagSelect from '@/components/AssetTagSelect.vue'

const tagLoading = ref(false)
const tags = ref([])
const classifications = ref([])
const filterClassification = ref('')
const tables = ref([])
const indicators = ref([])
const datasources = ref([])

const assetType = ref('TABLE')
const selectedAssetId = ref('')
const selectedTagIds = ref([])
const savingTags = ref(false)
const currentAssetTags = ref([])

const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const currentEditId = ref('')

const classifyVisible = ref(false)
const classifySaving = ref(false)
const classifyFormRef = ref(null)
const classifyForm = ref({ name: '', code: '', description: '' })
const classifyRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }]
}

const filteredTags = computed(() => {
  if (!filterClassification.value) return tags.value
  return tags.value.filter(t => t.classificationId === filterClassification.value)
})

const assetOptions = computed(() => {
  if (assetType.value === 'TABLE') return tables.value.map(t => ({ id: t.id, label: t.tableName }))
  if (assetType.value === 'INDICATOR') return indicators.value.map(i => ({ id: i.id, label: i.name }))
  if (assetType.value === 'DATASOURCE') return datasources.value.map(d => ({ id: d.id, label: d.name }))
  return []
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }]
}

const defaultForm = () => ({
  name: '',
  code: '',
  description: '',
  color: '#409EFF',
  classificationId: '',
  isMutuallyExclusive: false,
  status: 'DRAFT',
  derivedFromTagId: '',
  definition: '',
  synonyms: '',
  isTerm: false
})

const form = ref(defaultForm())

onMounted(async () => {
  fetchTags()
  fetchClassifications()
  try { tables.value = await catalogApi.list() || [] } catch (e) { /* ignore */ }
  try { indicators.value = await indicatorApi.list() || [] } catch (e) { /* ignore */ }
  try { datasources.value = await datasourceApi.list() || [] } catch (e) { /* ignore */ }
})

async function fetchTags() {
  tagLoading.value = true
  try {
    tags.value = await tagApi.list() || []
  } catch (e) {
    ElMessage.error('获取标签失败')
  } finally {
    tagLoading.value = false
  }
}

async function fetchClassifications() {
  try {
    classifications.value = await tagApi.classifications() || []
  } catch (e) {
    ElMessage.error('获取分类失败')
  }
}

function derivedName(id) {
  const t = tags.value.find(x => x.id === id)
  return t ? t.name : id
}

function openClassificationDialog() {
  classifyForm.value = { name: '', code: '', description: '' }
  classifyVisible.value = true
}

async function handleSaveClassification() {
  try { await classifyFormRef.value.validate() } catch (e) { return }
  classifySaving.value = true
  try {
    await tagApi.createClassification(classifyForm.value.name, classifyForm.value.code, classifyForm.value.description)
    ElMessage.success('新增成功')
    classifyVisible.value = false
    await fetchClassifications()
  } catch (e) {
    ElMessage.error('新增失败')
  } finally {
    classifySaving.value = false
  }
}

function resetAsset() {
  selectedAssetId.value = ''
  selectedTagIds.value = []
  currentAssetTags.value = []
}

async function loadAssetTags() {
  selectedTagIds.value = []
  try {
    currentAssetTags.value = await tagApi.bindings(assetType.value, selectedAssetId.value) || []
    selectedTagIds.value = currentAssetTags.value.map(b => b.tagId)
  } catch (e) {
    ElMessage.error('获取资产标签失败')
  }
}

async function saveTags() {
  if (!selectedAssetId.value) return
  savingTags.value = true
  try {
    const asset = assetOptions.value.find(a => a.id === selectedAssetId.value)
    await tagApi.bind({
      assetType: assetType.value,
      assetId: selectedAssetId.value,
      assetName: asset?.label || '',
      tagIds: selectedTagIds.value
    })
    ElMessage.success('打标成功')
    await loadAssetTags()
  } catch (e) {
    ElMessage.error('打标失败: ' + (e.message || '未知错误'))
  } finally {
    savingTags.value = false
  }
}

async function handleUnbind(binding) {
  try {
    await tagApi.unbind(binding.tagId, binding.assetType, binding.assetId)
    ElMessage.success('已解除标签')
    await loadAssetTags()
  } catch (e) {
    ElMessage.error('解除失败')
  }
}

function openAdd() {
  isEdit.value = false
  form.value = defaultForm()
  formVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  currentEditId.value = row.id
  form.value = {
    name: row.name,
    code: row.code,
    description: row.description,
    color: row.color,
    classificationId: row.classificationId || '',
    isMutuallyExclusive: !!row.isMutuallyExclusive,
    status: row.status || 'DRAFT',
    derivedFromTagId: row.derivedFromTagId || '',
    definition: row.definition || '',
    synonyms: row.synonyms || '',
    isTerm: !!row.isTerm
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
    const payload = { ...form.value }
    if (!payload.isTerm) payload.status = 'APPROVED'
    if (isEdit.value) {
      await tagApi.update(currentEditId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await tagApi.create(payload)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    await fetchTags()
  } catch (e) {
    ElMessage.error(isEdit.value ? '更新失败' : '新增失败')
  } finally {
    submitting.value = false
  }
}

async function handleSubmitTag(row) {
  try {
    await tagApi.submit(row.id)
    ElMessage.success('已提交审批')
    await fetchTags()
  } catch (e) {
    ElMessage.error('提交失败: ' + (e.message || '未知错误'))
  }
}

async function handleApprove(row) {
  try {
    await tagApi.approve(row.id)
    ElMessage.success('已通过')
    await fetchTags()
  } catch (e) {
    ElMessage.error('操作失败: ' + (e.message || '未知错误'))
  }
}

async function handleReject(row) {
  try {
    await tagApi.reject(row.id)
    ElMessage.success('已驳回')
    await fetchTags()
  } catch (e) {
    ElMessage.error('操作失败: ' + (e.message || '未知错误'))
  }
}

function statusText(status) {
  const map = { APPROVED: '已发布', DRAFT: '草稿', IN_REVIEW: '待审批', REJECTED: '已驳回' }
  return map[status] || status || '-'
}

function statusType(status) {
  const map = { APPROVED: 'success', DRAFT: 'info', IN_REVIEW: 'warning', REJECTED: 'danger' }
  return map[status] || 'info'
}

async function handleDelete(id) {
  try {
    await tagApi.delete(id)
    ElMessage.success('删除成功')
    await fetchTags()
    if (selectedAssetId.value) await loadAssetTags()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.tag-page {
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

.tag-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title-row .section-title {
  margin-bottom: 0;
}

.tag-toolbar {
  display: flex;
  gap: 8px;
}

.tag-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.asset-tag-select {
  width: 260px;
}

.tag-chip {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 13px;
}

.derived-tag {
  font-size: 12px;
  color: #6b7280;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.definition-cell {
  font-size: 12px;
  color: #6b7280;
}

.asset-tags-box {
  margin-top: 12px;
  background: #f8fafc;
  border-radius: 6px;
  padding: 12px;
}

.asset-tags-title {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 8px;
}

.asset-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.color-picker {
  display: flex;
  align-items: center;
}
</style>