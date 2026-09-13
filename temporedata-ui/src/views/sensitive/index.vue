<template>
  <div class="page">
    <PageHeader title="敏感数据治理" subtitle="敏感字段识别、分级与动态脱敏规则">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate(tab)">+ 新建</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- 敏感字段 -->
      <el-tab-pane label="敏感字段" name="sensitive">
        <el-skeleton v-if="loading" :rows="6" animated />
        <DataTable v-else :data="rows">
          <el-table-column prop="datasourceId" label="数据源" min-width="120">
            <template #default="{ row }"><span class="mono">{{ row.datasourceId || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="tableName" label="表名" min-width="150">
            <template #default="{ row }"><span class="mono">{{ row.tableName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="columnName" label="字段名" min-width="150">
            <template #default="{ row }"><span class="mono">{{ row.columnName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="sensitiveType" label="敏感类型" width="130">
            <template #default="{ row }"><el-tag size="small" :type="sTypeTag(row.sensitiveType)">{{ row.sensitiveType || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="level" label="级别" width="100">
            <template #default="{ row }">
              <StatusBadge :status="row.level ? 'failed' : 'success'" :text="row.level || '—'" />
            </template>
          </el-table-column>
          <el-table-column prop="maskRule" label="脱敏规则" width="110">
            <template #default="{ row }"><el-tag size="small" type="info">{{ row.maskRule || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
        <el-pagination
          v-if="total > size"
          class="pager"
          layout="total, prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="onPage"
        />
      </el-tab-pane>

      <!-- 脱敏规则 -->
      <el-tab-pane label="脱敏规则" name="rules">
        <el-skeleton v-if="ruleLoading" :rows="5" animated />
        <DataTable v-else :data="rules">
          <el-table-column prop="name" label="规则名称" min-width="140" />
          <el-table-column prop="ruleType" label="规则类型" width="130">
            <template #default="{ row }"><el-tag size="small">{{ row.ruleType }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="maskPattern" label="脱敏模式" min-width="160">
            <template #default="{ row }"><span class="mono">{{ row.maskPattern || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="tableName" label="表" min-width="140">
            <template #default="{ row }"><span class="mono">{{ row.tableName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="columnName" label="字段" min-width="140">
            <template #default="{ row }"><span class="mono">{{ row.columnName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <StatusBadge :status="row.status === 0 ? 'closed' : 'success'" :text="row.status === 0 ? '停用' : '启用'" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openEditRule(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="removeRule(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>
    </el-tabs>

    <!-- Sensitive dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑敏感字段' : '新增敏感字段'" width="540px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="数据源">
          <el-input v-model="form.datasourceId" placeholder="数据源 ID" />
        </el-form-item>
        <el-form-item label="表名" required>
          <el-input v-model="form.tableName" />
        </el-form-item>
        <el-form-item label="字段名" required>
          <el-input v-model="form.columnName" />
        </el-form-item>
        <el-form-item label="敏感类型">
          <el-select v-model="form.sensitiveType" style="width: 100%">
            <el-option v-for="t in ['PII', 'PHONE', 'EMAIL', 'ID_CARD', 'BANK_CARD', 'ADDRESS', 'HEALTH', 'FINANCE', 'OTHER']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="form.level" style="width: 100%">
            <el-option v-for="l in ['L2', 'L3', 'L4']" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="脱敏规则">
          <el-input v-model="form.maskRule" placeholder="如 MASK, 引用脱敏规则名" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- Mask rule dialog -->
    <el-dialog v-model="ruleVisible" :title="editingRule ? '编辑脱敏规则' : '新增脱敏规则'" width="540px">
      <el-form :model="ruleForm" label-width="90px">
        <el-form-item label="规则名称" required>
          <el-input v-model="ruleForm.name" />
        </el-form-item>
        <el-form-item label="规则类型" required>
          <el-select v-model="ruleForm.ruleType" style="width: 100%">
            <el-option v-for="t in ['REPLACE', 'MASK', 'HASH', 'EXTRACT', 'TRUNCATE', 'DERIVE']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="脱敏模式">
          <el-input v-model="ruleForm.maskPattern" placeholder="如 *** / \u0040@@@@"/>
        </el-form-item>
        <el-form-item label="数据源">
          <el-input v-model="ruleForm.datasourceId" />
        </el-form-item>
        <el-form-item label="表名">
          <el-input v-model="ruleForm.tableName" />
        </el-form-item>
        <el-form-item label="字段名">
          <el-input v-model="ruleForm.columnName" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="ruleForm.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="ruleForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRule">保存</el-button>
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
import { maskRuleApi, sensitiveApi, type MaskRuleEntity, type SensitiveEntity } from '@/api/sensitive'

const queryClient = useQueryClient()
const tab = ref('sensitive')

// sensitive list
const rows = ref<SensitiveEntity[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)

// rules
const rules = ref<MaskRuleEntity[]>([])
const ruleLoading = ref(false)

// dialogs
const dialogVisible = ref(false)
const editing = ref<SensitiveEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<SensitiveEntity>>({})

const ruleVisible = ref(false)
const editingRule = ref<MaskRuleEntity | null>(null)
const ruleForm = ref<Partial<MaskRuleEntity>>({})

async function loadSensitive() {
  loading.value = true
  try {
    const res = await sensitiveApi.page({ page: page.value - 1, size })
    rows.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}
async function loadRules() {
  ruleLoading.value = true
  try {
    rules.value = await maskRuleApi.list()
  } finally {
    ruleLoading.value = false
  }
}
loadSensitive()

function onPage(p: number) {
  page.value = p
  loadSensitive()
}
function onTabChange(name: string | number) {
  if (name === 'rules') loadRules()
}

function openCreate(type: string) {
  editing.value = null
  editingRule.value = null
  if (type === 'sensitive') {
    form.value = { datasourceId: '', tableName: '', columnName: '', sensitiveType: 'PII', level: 'L3', maskRule: '', description: '' }
    dialogVisible.value = true
  } else {
    ruleForm.value = { name: '', ruleType: 'MASK', maskPattern: '***', description: '', status: 1 }
    ruleVisible.value = true
  }
}
function openEdit(row: SensitiveEntity) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}
function openEditRule(row: MaskRuleEntity) {
  editingRule.value = row
  ruleForm.value = { ...row }
  ruleVisible.value = true
}

async function save() {
  if (!form.value.tableName || !form.value.columnName) {
    ElMessage.warning('表名与字段名必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) await sensitiveApi.update(form.value)
    else await sensitiveApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['sensitive'] })
    await loadSensitive()
  } finally {
    saving.value = false
  }
}

async function saveRule() {
  if (!ruleForm.value.name || !ruleForm.value.ruleType) {
    ElMessage.warning('名称与规则类型必填')
    return
  }
  saving.value = true
  try {
    if (editingRule.value) await maskRuleApi.update(editingRule.value.id, ruleForm.value)
    else await maskRuleApi.create(ruleForm.value)
    ElMessage.success('保存成功')
    ruleVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['maskrule'] })
    await loadRules()
  } finally {
    saving.value = false
  }
}

async function remove(row: SensitiveEntity) {
  await ElMessageBox.confirm(`确认删除敏感字段 ${row.tableName}.${row.columnName} ？`, '提示', { type: 'warning' })
  await sensitiveApi.remove(row.id)
  ElMessage.success('已删除')
  await loadSensitive()
}
async function removeRule(row: MaskRuleEntity) {
  await ElMessageBox.confirm(`确认删除脱敏规则 ${row.name} ？`, '提示', { type: 'warning' })
  await maskRuleApi.remove(row.id)
  ElMessage.success('已删除')
  await loadRules()
}

function sTypeTag(t?: string) {
  if (t === 'ID_CARD' || t === 'BANK_CARD' || t === 'HEALTH') return 'danger'
  if (t === 'PHONE' || t === 'EMAIL' || t === 'FINANCE') return 'warning'
  return 'info'
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
</style>