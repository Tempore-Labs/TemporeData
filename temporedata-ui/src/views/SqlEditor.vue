<template>
  <div class="sql-editor">
    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select
          v-model="selectedDatasourceId"
          placeholder="请选择数据源"
          size="default"
          style="width: 240px"
          clearable
        >
          <el-option
            v-for="ds in datasources"
            :key="ds.id"
            :label="ds.name"
            :value="ds.id"
          >
            <span style="float: left">{{ ds.name }}</span>
            <el-tag size="small" style="float: right; margin-left: 8px" type="info">{{ ds.type }}</el-tag>
          </el-option>
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button @click="handleFormat" :icon="Operation">格式化</el-button>
        <el-button :icon="Share" @click="handleLineage" :loading="lineaging">血缘</el-button>
        <el-button type="primary" @click="handleExecute" :icon="VideoPlay" :loading="executing">
          {{ executing ? '执行中...' : '执行' }}
        </el-button>
      </div>
    </div>

    <!-- Panels -->
    <div class="panels">
      <!-- Left: SQL Editor -->
      <div class="editor-panel">
        <div class="panel-header">
          <span class="panel-title">SQL 编辑器</span>
          <span class="line-count">{{ sqlLineCount }} 行</span>
        </div>
        <textarea
          ref="editorRef"
          v-model="sql"
          class="sql-textarea"
          placeholder="请输入 SQL 查询语句..."
          spellcheck="false"
          @keydown.tab.prevent="handleTab"
        ></textarea>
      </div>

      <!-- Right: Results -->
      <div class="result-panel">
        <div class="panel-header">
          <span class="panel-title">查询结果</span>
          <span v-if="resultInfo" class="result-info">
            <el-tag size="small" type="success">{{ resultInfo.rowCount }} 行</el-tag>
            <el-tag size="small" type="info" style="margin-left: 6px">{{ resultInfo.duration }}ms</el-tag>
          </span>
        </div>

        <!-- Empty State -->
        <div v-if="!hasResult && !executing" class="empty-state">
          <el-icon class="empty-icon"><DataAnalysis /></el-icon>
          <p>选择数据源并输入 SQL 后点击执行</p>
        </div>

        <!-- Error State -->
        <div v-if="errorMsg" class="error-state">
          <el-alert :title="errorMsg" type="error" show-icon :closable="false" />
        </div>

        <!-- Result Table -->
        <div v-if="hasResult" class="result-table-wrap">
          <el-table
            :data="resultRows"
            border
            stripe
            size="small"
            style="width: 100%"
            max-height="100%"
            empty-text="查询成功，但无返回数据"
          >
            <el-table-column
              v-for="col in resultColumns"
              :key="col"
              :prop="col"
              :label="col"
              :min-width="120"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <span class="cell-value">{{ row[col] }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- SQL Lineage Dialog -->
    <el-dialog v-model="lineageVisible" title="SQL 血缘解析" width="560px" :close-on-click-modal="false">
      <template v-if="lineageResult">
        <div class="lineage-head">
          <el-tag type="primary" size="small">{{ lineageResult.sqlType }}</el-tag>
          <span class="lineage-arrow">{{ (lineageResult.sources || []).join(', ') || '(未解析到来源表)' }} → {{ lineageResult.targetTable || '(查询)' }}</span>
        </div>
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="目标表">{{ lineageResult.targetTable || '-' }}</el-descriptions-item>
          <el-descriptions-item label="来源表">
            <el-tag v-for="t in lineageResult.sources" :key="t" size="small" style="margin-right: 6px">{{ t }}</el-tag>
            <span v-if="!lineageResult.sources.length">-</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <p v-else class="lineage-empty">输入 SQL 后点击「血缘」解析其中的表间依赖</p>
      <template #footer>
        <el-button @click="lineageVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { VideoPlay, Operation, DataAnalysis, Share } from '@element-plus/icons-vue'
import { queryApi } from '@/api/modules/query'
import { datasourceApi } from '@/api/modules/datasource'
import { workflowLineageApi } from '@/api/modules/workflowLineage'
import { ElMessage } from 'element-plus'

// ---- State ----
const sql = ref('')
const selectedDatasourceId = ref(null)
const datasources = ref([])
const executing = ref(false)
const errorMsg = ref('')
const lineaging = ref(false)
const lineageVisible = ref(false)
const lineageResult = ref(null)

const resultColumns = ref([])
const resultRows = ref([])
const resultDuration = ref(0)

const editorRef = ref(null)

// ---- Computed ----
const sqlLineCount = computed(() => {
  if (!sql.value) return 0
  return sql.value.split('\n').length
})

const hasResult = computed(() => resultColumns.value.length > 0)

const resultInfo = computed(() => {
  if (!hasResult.value) return null
  return {
    rowCount: resultRows.value.length,
    duration: resultDuration.value
  }
})

// ---- Lifecycle ----
onMounted(() => {
  loadDatasources()
})

// ---- Methods ----
async function loadDatasources() {
  try {
    datasources.value = await datasourceApi.list()
  } catch (err) {
    ElMessage.error('加载数据源列表失败: ' + (err.message || '未知错误'))
  }
}

function handleTab() {
  const textarea = editorRef.value
  if (!textarea) return
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  sql.value = sql.value.substring(0, start) + '  ' + sql.value.substring(end)
  // Restore cursor position after the inserted tab
  requestAnimationFrame(() => {
    textarea.selectionStart = textarea.selectionEnd = start + 2
  })
}

function handleFormat() {
  if (!sql.value.trim()) return
  const keywords = [
    'SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'NOT', 'IN', 'IS', 'NULL',
    'JOIN', 'LEFT', 'RIGHT', 'INNER', 'OUTER', 'FULL', 'CROSS', 'ON',
    'GROUP', 'BY', 'ORDER', 'ASC', 'DESC', 'HAVING', 'LIMIT', 'OFFSET',
    'INSERT', 'INTO', 'VALUES', 'UPDATE', 'SET', 'DELETE', 'CREATE',
    'TABLE', 'ALTER', 'DROP', 'INDEX', 'VIEW', 'AS', 'DISTINCT',
    'BETWEEN', 'LIKE', 'EXISTS', 'UNION', 'ALL', 'CASE', 'WHEN',
    'THEN', 'ELSE', 'END', 'COUNT', 'SUM', 'AVG', 'MIN', 'MAX',
    'WITH', 'RECURSIVE', 'CAST', 'COALESCE', 'NULLIF', 'TRUNCATE',
    'PRIMARY', 'KEY', 'FOREIGN', 'REFERENCES', 'DEFAULT', 'CHECK',
    'UNIQUE', 'CONSTRAINT', 'CASCADE', 'IF', 'EXCEPT', 'INTERSECT'
  ]

  let formatted = sql.value
  keywords.forEach(kw => {
    const regex = new RegExp('\\b' + kw + '\\b', 'gi')
    formatted = formatted.replace(regex, kw)
  })
  sql.value = formatted
  ElMessage.success('格式化完成')
}

async function handleLineage() {
  if (!sql.value.trim()) {
    ElMessage.warning('请输入 SQL')
    return
  }
  lineaging.value = true
  try {
    lineageResult.value = await workflowLineageApi.parse(sql.value)
    lineageVisible.value = true
  } catch (e) {
    ElMessage.error('血缘解析失败：' + (e.message || ''))
  } finally { lineaging.value = false }
}

async function handleExecute() {
  if (!sql.value.trim()) {
    ElMessage.warning('请输入 SQL 语句')
    return
  }
  if (!selectedDatasourceId.value) {
    ElMessage.warning('请先选择数据源')
    return
  }

  executing.value = true
  errorMsg.value = ''
  resultColumns.value = []
  resultRows.value = []
  resultDuration.value = 0

  try {
    const data = await queryApi.execute(sql.value, selectedDatasourceId.value)
    if (data) {
      resultColumns.value = data.columns || []
      resultRows.value = data.rows || []
      resultDuration.value = data.duration ?? 0
    }
  } catch (err) {
    errorMsg.value = err.response?.data?.msg || err.message || '查询执行失败'
  } finally {
    executing.value = false
  }
}
</script>

<style scoped>
.sql-editor {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 112px);
  gap: 12px;
}

/* ---- Toolbar ---- */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 10px 16px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ---- Panels ---- */
.panels {
  display: flex;
  flex: 1;
  gap: 12px;
  min-height: 0;
}

/* ---- Editor Panel ---- */
.editor-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  min-width: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}
.line-count {
  font-size: 12px;
  color: #9ca3af;
}

.sql-textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  padding: 16px;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  color: #e2e8f0;
  background: #1e293b;
  tab-size: 2;
  min-height: 0;
}
.sql-textarea::placeholder {
  color: #64748b;
}
.sql-textarea:focus {
  outline: none;
}

/* ---- Result Panel ---- */
.result-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  min-width: 0;
}

.result-info {
  display: flex;
  align-items: center;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  gap: 12px;
}
.empty-icon {
  font-size: 48px;
  color: #d1d5db;
}
.empty-state p {
  font-size: 14px;
  margin: 0;
}

.error-state {
  padding: 16px;
}

.result-table-wrap {
  flex: 1;
  overflow: auto;
  padding: 0 1px 1px;
  min-height: 0;
}

.cell-value {
  font-size: 13px;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  color: #374151;
}

.lineage-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.lineage-arrow {
  font-size: 13px;
  color: #374151;
  word-break: break-all;
}
.lineage-empty {
  color: #9ca3af;
  font-size: 13px;
  margin: 0;
}
</style>