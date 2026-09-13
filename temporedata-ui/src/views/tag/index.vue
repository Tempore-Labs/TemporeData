<template>
  <div class="page">
    <PageHeader title="数据标签与指标" subtitle="数据资产标签体系与业务指标管理">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate(tab)">+ 新建</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- 标签管理 -->
      <el-tab-pane label="标签管理" name="tags">
        <el-skeleton v-if="tagLoading" :rows="6" animated />
        <DataTable v-else :data="tags">
          <el-table-column prop="name" label="标签名" min-width="130">
            <template #default="{ row }"><el-tag size="small" :color="row.color || undefined" effect="light">{{ row.name }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="code" label="编码" min-width="140">
            <template #default="{ row }"><span class="mono">{{ row.code || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="classificationName" label="分类" width="120">
            <template #default="{ row }">{{ row.classificationName || '—' }}</template>
          </el-table-column>
          <el-table-column label="互斥" width="90">
            <template #default="{ row }">
              <StatusBadge :status="row.isMutuallyExclusive === true ? 'failed' : 'success'" :text="row.isMutuallyExclusive === true ? '是' : '否'" />
            </template>
          </el-table-column>
          <el-table-column label="业务术语" width="100">
            <template #default="{ row }">
              <StatusBadge :status="row.isTerm === true ? 'success' : 'pending'" :text="row.isTerm === true ? '是' : '否'" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="delTag(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
        <el-pagination
          v-if="tagTotal > 10"
          class="pager"
          layout="total, prev, pager, next"
          :total="tagTotal"
          :page-size="10"
          :current-page="tagPage"
          @current-change="(p: number) => { tagPage = p; loadTags() }"
        />
      </el-tab-pane>

      <!-- 指标管理 -->
      <el-tab-pane label="指标管理" name="indicators">
        <el-skeleton v-if="indLoading" :rows="6" animated />
        <DataTable v-else :data="indicators">
          <el-table-column prop="name" label="指标名" min-width="140">
            <template #default="{ row }"><span class="mono">{{ row.name }}</span></template>
          </el-table-column>
          <el-table-column prop="code" label="编码" min-width="130">
            <template #default="{ row }"><span class="mono">{{ row.code || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="110">
            <template #default="{ row }"><el-tag size="small">{{ row.type || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="latestValue" label="最新值" min-width="130">
            <template #default="{ row }"><span class="mono" style="color: var(--td-primary)">{{ row.latestValue ?? '—' }}</span></template>
          </el-table-column>
          <el-table-column label="运行状态" width="110">
            <template #default="{ row }"><StatusBadge :status="row.latestStatus || row.status || 'pending'" /></template>
          </el-table-column>
          <el-table-column prop="owner" label="Owner" width="110" />
          <el-table-column label="成功率" width="110">
            <template #default="{ row }">
              <span class="mono" v-if="row.totalRuns">{{ successRate(row) }}%</span>
              <span class="mono muted" v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="delIndicator(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
        <el-pagination
          v-if="indTotal > 10"
          class="pager"
          layout="total, prev, pager, next"
          :total="indTotal"
          :page-size="10"
          :current-page="indPage"
          @current-change="(p: number) => { indPage = p; loadIndicators() }"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- Tag dialog -->
    <el-dialog v-model="tagVisible" :title="editingTag ? '编辑标签' : '新建标签'" width="520px">
      <el-form :model="tagForm" label-width="100px">
        <el-form-item label="标签名" required>
          <el-input v-model="tagForm.name" />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="tagForm.code" :disabled="!!editingTag" placeholder="如 tag_core" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="tagForm.color" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="tagForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="tagForm.classificationName" />
        </el-form-item>
        <el-form-item label="互斥">
          <el-switch v-model="tagForm.isMutuallyExclusive" />
        </el-form-item>
        <el-form-item label="业务术语">
          <el-switch v-model="tagForm.isTerm" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTag">保存</el-button>
      </template>
    </el-dialog>

    <!-- Indicator dialog -->
    <el-dialog v-model="indVisible" :title="editingInd ? '编辑指标' : '新建指标'" width="560px">
      <el-form :model="indForm" label-width="100px">
        <el-form-item label="指标名" required>
          <el-input v-model="indForm.name" />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="indForm.code" :disabled="!!editingInd" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="indForm.type" style="width: 100%">
            <el-option v-for="t in ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX', 'RATIO', 'CUSTOM']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="查询 SQL">
          <el-input v-model="indForm.querySql" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="indForm.unit" />
        </el-form-item>
        <el-form-item label="Owner">
          <el-input v-model="indForm.owner" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="indForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="indVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveIndicator">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { indicatorApi, tagApi, type IndicatorEntity, type TagEntity } from '@/api/tag'

const queryClient = useQueryClient()
const tab = ref('tags')

// tags
const tags = ref<TagEntity[]>([])
const tagLoading = ref(false)
const tagPage = ref(1)
const tagTotal = ref(0)

// indicators
const indicators = ref<IndicatorEntity[]>([])
const indLoading = ref(false)
const indPage = ref(1)
const indTotal = ref(0)

const tagVisible = ref(false)
const editingTag = ref<TagEntity | null>(null)
const tagForm = ref<Partial<TagEntity>>({})

const indVisible = ref(false)
const editingInd = ref<IndicatorEntity | null>(null)
const indForm = ref<Partial<IndicatorEntity>>({})

const saving = ref(false)

async function loadTags() {
  tagLoading.value = true
  try {
    const res = await tagApi.page({ page: tagPage.value - 1, size: 10 })
    tags.value = res.content || []
    tagTotal.value = res.totalElements || 0
  } finally {
    tagLoading.value = false
  }
}
async function loadIndicators() {
  indLoading.value = true
  try {
    const res = await indicatorApi.page({ page: indPage.value - 1, size: 10 })
    indicators.value = res.content || []
    indTotal.value = res.totalElements || 0
  } finally {
    indLoading.value = false
  }
}
loadTags()

function onTabChange(name: string | number) {
  if (name === 'indicators' && indicators.value.length === 0) loadIndicators()
}

function openCreate(type: string) {
  if (type === 'tags') {
    editingTag.value = null
    tagForm.value = { name: '', code: '', color: undefined, description: '', classificationName: '', isMutuallyExclusive: false, isTerm: false }
    tagVisible.value = true
  } else {
    editingInd.value = null
    indForm.value = { name: '', code: '', type: 'SUM', querySql: '', unit: '', owner: '', description: '' }
    indVisible.value = true
  }
}
function openEdit(row: TagEntity | IndicatorEntity) {
  if (tab.value === 'tags') {
    editingTag.value = row as TagEntity
    tagForm.value = { ...(row as TagEntity) }
    tagVisible.value = true
  } else {
    editingInd.value = row as IndicatorEntity
    indForm.value = { ...(row as IndicatorEntity) }
    indVisible.value = true
  }
}

async function saveTag() {
  if (!tagForm.value.name || !tagForm.value.code) {
    ElMessage.warning('名称与编码必填')
    return
  }
  saving.value = true
  try {
    if (editingTag.value) await tagApi.update(tagForm.value)
    else await tagApi.create(tagForm.value)
    ElMessage.success('保存成功')
    tagVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['tag'] })
    await loadTags()
  } finally {
    saving.value = false
  }
}
async function saveIndicator() {
  if (!indForm.value.name || !indForm.value.code) {
    ElMessage.warning('名称与编码必填')
    return
  }
  saving.value = true
  try {
    if (editingInd.value) await indicatorApi.update(indForm.value)
    else await indicatorApi.create(indForm.value)
    ElMessage.success('保存成功')
    indVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['indicator'] })
    await loadIndicators()
  } finally {
    saving.value = false
  }
}

async function delTag(row: TagEntity) {
  await ElMessageBox.confirm(`确认删除标签 ${row.name} ？`, '提示', { type: 'warning' })
  await tagApi.remove(row.id)
  ElMessage.success('已删除')
  await loadTags()
}
async function delIndicator(row: IndicatorEntity) {
  await ElMessageBox.confirm(`确认删除指标 ${row.name} ？`, '提示', { type: 'warning' })
  await indicatorApi.remove(row.id)
  ElMessage.success('已删除')
  await loadIndicators()
}

function successRate(row: IndicatorEntity) {
  const t = row.totalRuns || 0
  if (!t) return 0
  return Math.round(((row.successRuns || 0) / t) * 100)
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.mono {
  font-family: var(--td-font-mono);
}
.muted {
  color: var(--td-text-4);
}
</style>