<template>
  <div class="quality-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>数据质量规则</h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        新增规则
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table :data="pagedData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="规则名称" min-width="160" />
        <el-table-column prop="ruleType" label="规则类型" width="140">
          <template #default="{ row }">
            <el-tag :type="ruleTypeTag(row.ruleType)" effect="light">{{ row.ruleType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetTable" label="目标表" min-width="140" />
        <el-table-column prop="targetColumn" label="目标列" min-width="120" />
        <el-table-column label="表达式" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <code class="expression-code">{{ row.expression }}</code>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="dark">
              <el-icon style="margin-right: 4px; vertical-align: middle;">
                <CircleCheck v-if="row.status === 'ENABLED'" />
              </el-icon>
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openEditDialog(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button size="small" type="success" link @click="handleExecute(row)">
              <el-icon><CircleCheck /></el-icon>
              执行
            </el-button>
            <el-popconfirm title="确定删除该规则吗？" confirm-button-text="确定" cancel-button-text="取消" @confirm="handleDelete(row)">
              <template #reference>
                <el-button size="small" type="danger" link>
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="tableData.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑规则' : '新增规则'"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="right">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入规则名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="form.ruleType" placeholder="请选择规则类型" style="width: 100%" @change="onRuleTypeChange">
            <el-option label="非空校验 (NOT_NULL)" value="NOT_NULL" />
            <el-option label="唯一性校验 (UNIQUE)" value="UNIQUE" />
            <el-option label="范围校验 (RANGE)" value="RANGE" />
            <el-option label="正则校验 (REGEX)" value="REGEX" />
            <el-option label="自定义SQL (CUSTOM_SQL)" value="CUSTOM_SQL" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源" prop="datasourceId">
          <el-select v-model="form.datasourceId" placeholder="请选择数据源" style="width: 100%" filterable @change="onDatasourceChange">
            <el-option v-for="ds in dataSources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标表" prop="targetTable">
          <el-select v-model="form.targetTable" placeholder="请选择目标表" style="width: 100%" filterable @change="onTableChange">
            <el-option v-for="t in tableOptions" :key="t.name" :label="t.name" :value="t.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标列" prop="targetColumn">
          <el-select v-model="form.targetColumn" placeholder="请选择目标列" style="width: 100%" filterable clearable>
            <el-option v-for="c in tableColumns" :key="c.columnName" :label="c.columnName" :value="c.columnName" />
          </el-select>
        </el-form-item>
        <el-form-item label="表达式" prop="expression">
          <el-input v-model="form.expression" :placeholder="expressionPlaceholder" type="textarea" :rows="3" />
          <div class="expression-hint" v-if="form.ruleType">
            <el-icon><InfoFilled /></el-icon>
            <span>{{ expressionHint }}</span>
          </div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" placeholder="请输入规则描述" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 执行结果弹窗（真实通过率 + 异常样本） -->
    <el-dialog v-model="runDialogVisible" title="质量校验结果" width="560px" :close-on-click-modal="false">
      <div v-if="runResult" class="run-panel">
        <div class="run-metrics">
          <div class="metric">
            <span class="metric-label">通过率</span>
            <span class="metric-value" :class="runResult.passRate >= 1 ? 'ok' : 'bad'">{{ ratePct(runResult.passRate) }}</span>
          </div>
          <div class="metric">
            <span class="metric-label">总行数</span>
            <span class="metric-value">{{ runResult.totalRows ?? '-' }}</span>
          </div>
          <div class="metric">
            <span class="metric-label">异常行</span>
            <span class="metric-value bad">{{ runResult.badRows ?? '-' }}</span>
          </div>
          <div class="metric">
            <span class="metric-label">状态</span>
            <el-tag :type="runResult.status === 'PASS' ? 'success' : 'danger'" size="small">{{ runResult.status }}</el-tag>
          </div>
        </div>
        <el-alert v-if="runResult.errorMsg" type="error" :closable="false" show-icon :title="runResult.errorMsg" style="margin-top: 12px" />
        <template v-if="runResult.badSample && runResult.badSample.length">
          <h4 class="sample-title">异常样本（前 {{ runResult.badSample.length }} 条）</h4>
          <div class="sample-list">
            <el-tag v-for="(s, i) in runResult.badSample" :key="i" type="danger" effect="plain" class="sample-item">{{ s }}</el-tag>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="runDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { qualityApi } from '@/api/modules/quality'
import { datasourceApi } from '@/api/modules/datasource'
import { catalogApi } from '@/api/modules/catalog'
import { ElMessage } from 'element-plus'

// ---- 表格数据 ----
const tableData = ref([])
const loading = ref(false)

// ---- 表单联动数据 ----
const dataSources = ref([])
const tableOptions = ref([])
const tableColumns = ref([])

async function fetchDatasources() {
  try {
    dataSources.value = await datasourceApi.list()
  } catch (err) {
    ElMessage.error(err.message || '加载数据源失败')
  }
}

async function loadTables(datasourceId) {
  tableOptions.value = []
  tableColumns.value = []
  form.targetTable = ''
  form.targetColumn = ''
  if (!datasourceId) return
  try {
    const tree = await catalogApi.tree()
    const ds = tree.find(d => d.id === datasourceId)
    for (const schema of ds?.children || []) {
      for (const t of schema?.children || []) {
        tableOptions.value.push({ name: t.label, id: t.id })
      }
    }
  } catch (err) {
    ElMessage.error(err.message || '加载表列表失败')
  }
}

function onDatasourceChange() {
  loadTables(form.datasourceId)
}

async function onTableChange() {
  tableColumns.value = []
  form.targetColumn = ''
  const opt = tableOptions.value.find(t => t.name === form.targetTable)
  if (!opt) return
  try {
    const detail = await catalogApi.detail(opt.id)
    tableColumns.value = detail?.columns || []
  } catch (err) {
    ElMessage.error(err.message || '加载字段列表失败')
  }
}

// ---- 分页 ----
const currentPage = ref(1)
const pageSize = ref(10)

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return tableData.value.slice(start, start + pageSize.value)
})

// ---- 弹窗 ----
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)

const defaultForm = () => ({
  name: '',
  ruleType: '',
  datasourceId: '',
  targetTable: '',
  targetColumn: '',
  expression: '',
  description: ''
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  targetTable: [{ required: true, message: '请选择目标表', trigger: 'change' }],
  targetColumn: [{ required: true, message: '请选择目标列', trigger: 'change' }],
  expression: [{
    validator: (rule, value, cb) => {
      if (['NOT_NULL', 'UNIQUE'].includes(form.ruleType)) return cb()
      if (!value) return cb(new Error('请输入表达式'))
      cb()
    },
    trigger: 'blur'
  }]
}

// ---- 表达式提示 ----
const expressionHint = computed(() => {
  const hints = {
    NOT_NULL: '无需填写表达式，非空校验自动生效',
    UNIQUE: '无需填写表达式，唯一性校验自动生效',
    RANGE: '请填写范围，格式：min,max（如：0,100）',
    REGEX: '请填写正则表达式模式（如：^[a-zA-Z]+$）',
    CUSTOM_SQL: '请填写自定义SQL表达式（如：column > 0 AND column IS NOT NULL）'
  }
  return hints[form.ruleType] || '请输入表达式'
})

const expressionPlaceholder = computed(() => {
  const placeholders = {
    NOT_NULL: '非空校验无需表达式',
    UNIQUE: '唯一性校验无需表达式',
    RANGE: 'min,max',
    REGEX: '正则表达式模式',
    CUSTOM_SQL: 'SQL表达式'
  }
  return placeholders[form.ruleType] || '请输入表达式'
})

function onRuleTypeChange() {
  form.expression = ''
}

// ---- 规则类型标签颜色 ----
function ruleTypeTag(type) {
  const map = {
    NOT_NULL: 'danger',
    UNIQUE: 'warning',
    RANGE: 'success',
    REGEX: '',
    CUSTOM_SQL: 'info'
  }
  return map[type] || 'info'
}

// ---- 加载数据 ----
async function fetchData() {
  loading.value = true
  try {
    tableData.value = await qualityApi.list()
  } catch (err) {
    ElMessage.error(err.message || '加载质量规则列表失败')
  } finally {
    loading.value = false
  }
}

// ---- 新增 ----
function openAddDialog() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, defaultForm())
  fetchDatasources()
  dialogVisible.value = true
}

// ---- 编辑 ----
function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    name: row.name,
    ruleType: row.ruleType,
    datasourceId: row.datasourceId || '',
    targetTable: row.tableName || '',
    targetColumn: row.columnName || '',
    expression: row.ruleConfig || '',
    description: row.description || ''
  })
  fetchDatasources()
  if (row.datasourceId) loadTables(row.datasourceId)
  dialogVisible.value = true
}

// ---- 提交 ----
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = {
      name: form.name,
      ruleType: form.ruleType,
      datasourceId: form.datasourceId,
      tableName: form.targetTable,
      columnName: form.targetColumn,
      ruleConfig: form.expression,
      description: form.description
    }
    if (isEdit.value) {
      await qualityApi.update(editId.value, payload)
      ElMessage.success('规则更新成功')
    } else {
      await qualityApi.create(payload)
      ElMessage.success('规则创建成功')
    }
    dialogVisible.value = false
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// ---- 执行（真实引擎 + 结果面板） ----
const runDialogVisible = ref(false)
const runResult = ref(null)
const runLoading = ref(false)

async function handleExecute(row) {
  runLoading.value = true
  try {
    runResult.value = await qualityApi.run(row.id)
    runDialogVisible.value = true
  } catch (err) {
    ElMessage.error(err.message || '规则执行失败')
  } finally {
    runLoading.value = false
  }
}

function ratePct(rate) {
  return rate == null ? '-' : (rate * 100).toFixed(1) + '%'
}

// ---- 删除 ----
async function handleDelete(row) {
  try {
    await qualityApi.delete(row.id)
    ElMessage.success('规则已删除')
    const remaining = tableData.value.length - 1
    if (pagedData.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

// ---- 初始化 ----
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.quality-page {
  padding: 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.expression-code {
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  color: #374151;
}

.expression-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.expression-hint .el-icon {
  font-size: 14px;
  flex-shrink: 0;
}

/* ---- Run result panel ---- */
.run-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 16px;
  background: #f7f8fa;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}
.metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}
.metric-label {
  font-size: 12px;
  color: #9ca3af;
}
.metric-value {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}
.metric-value.ok {
  color: #10b981;
}
.metric-value.bad {
  color: #ef4444;
}
.sample-title {
  margin: 16px 0 8px;
  font-size: 13px;
  color: #6b7280;
}
.sample-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.sample-item {
  font-family: 'Menlo', 'Monaco', monospace;
}
</style>