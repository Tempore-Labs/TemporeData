<template>
  <div class="sql-page">
    <!-- Toolbar -->
    <div class="toolbar">
      <el-select v-model="datasource" size="small" style="width: 180px">
        <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.name" />
      </el-select>

      <el-button size="small" @click="formatSql">格式化</el-button>
      <el-button size="small" :loading="lineageLoading" @click="parseLineage">血缘解析</el-button>
      <el-button size="small" @click="openHistory">历史</el-button>
      <el-button
        size="small"
        type="primary"
        :loading="running"
        @click="execute"
      >
        <el-icon v-if="!running" :size="12" style="margin-right: 4px"><CaretRight /></el-icon>
        {{ running ? '执行中...' : '执行 (⌘↵)' }}
      </el-button>

      <div class="flex-1" />

      <template v-if="result && result.rows.length">
        <span class="meta-chip"><span class="dot ok" /> {{ result.rows.length }} 行结果</span>
        <span class="meta-chip">耗时 {{ result.elapsed }}</span>
      </template>
    </div>

    <!-- Editor + results -->
    <div class="main">
      <div class="editor-pane">
        <div class="editor-tab">
          <span class="traffic" /><span class="traffic amber" /><span class="traffic green" />
          <span class="file-name">query.sql</span>
        </div>
        <div ref="editorEl" class="monaco-host" />
      </div>

      <div class="result-pane">
        <div class="result-head">
          <span class="result-title">查询结果</span>
          <span v-if="result" class="result-sub">显示前 {{ result.rows.length }} 行</span>
          <div class="flex-1" />
          <el-button v-if="result" size="small" text type="primary" @click="exportCsv">导出 CSV</el-button>
        </div>

        <div class="result-body">
          <el-alert
            v-if="lastError"
            type="error"
            :title="lastError"
            :closable="false"
            show-icon
            style="margin: 12px"
          />
          <el-table v-else-if="result && result.columns.length" :data="result.rows" size="small" max-height="100%" table-layout="fixed">
            <el-table-column
              v-for="c in result.columns"
              :key="c"
              :prop="c"
              :label="c"
              min-width="120"
            >
              <template #default="{ row }">
                <span class="mono">{{ row[c] }}</span>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="result-empty">
            <div class="re-icon"><el-icon :size="26"><EditPen /></el-icon></div>
            <div>执行 SQL 查看结果</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Query history drawer -->
    <el-drawer v-model="historyVisible" title="查询历史" size="560px">
      <el-skeleton v-if="historyLoading" :rows="8" animated />
      <div v-else class="history-list">
        <div v-for="h in historyRows" :key="h.id" class="history-item" @click="applyHistory(h)">
          <div class="history-head">
            <StatusBadge :status="h.status" />
            <span class="history-time mono">{{ h.createTime || '—' }}</span>
          </div>
          <div class="history-sql mono">{{ h.sql }}</div>
          <div class="history-meta">
            <span>{{ h.datasourceName || '—' }}</span>
            <span>{{ h.rowCount }} 行 · {{ (h.durationMs / 1000).toFixed(2) }}s</span>
          </div>
        </div>
        <el-empty v-if="historyRows.length === 0" description="暂无历史记录" :image-size="60" />
      </div>
    </el-drawer>

    <!-- Lineage drawer -->
    <el-drawer v-model="lineageVisible" title="SQL 血缘解析" size="520px">
      <template v-if="lineageResult">
        <el-alert
          v-if="lineageResult.success === false"
          type="error"
          :title="lineageResult.message || '解析失败'"
          :closable="false"
          show-icon
        />
        <template v-else>
          <div class="lg-row"><span class="lg-label">语句类型</span><el-tag size="small" effect="plain">{{ lineageResult.sqlType || '—' }}</el-tag></div>
          <div class="lg-row"><span class="lg-label">目标表</span><span class="mono">{{ lineageResult.targetTable || '—（SELECT 无产出表）' }}</span></div>
          <div class="lg-row"><span class="lg-label">读取源表</span>
            <div class="lg-tags">
              <el-tag v-if="!lineageResult.sources?.length" size="small" type="info" effect="plain">无</el-tag>
              <el-tag v-for="s in lineageResult.sources" :key="s" size="small" effect="plain" class="lg-tag">{{ s }}</el-tag>
            </div>
          </div>
          <div class="lg-row"><span class="lg-label">引用的表</span>
            <div class="lg-tags">
              <el-tag v-if="!lineageResult.tables?.length" size="small" type="info" effect="plain">无</el-tag>
              <el-tag v-for="t in lineageResult.tables" :key="t" size="small" effect="plain" class="lg-tag">{{ t }}</el-tag>
            </div>
          </div>
          <div class="lg-row"><span class="lg-label">字段</span>
            <div class="lg-tags">
              <el-tag v-if="!lineageResult.columns?.length" size="small" type="info" effect="plain">无</el-tag>
              <el-tag v-for="c in lineageResult.columns" :key="c" size="small" type="warning" effect="plain" class="lg-tag">{{ c }}</el-tag>
            </div>
          </div>
          <el-divider content-position="left">列级血缘（目标 ← 源）</el-divider>
          <el-table v-if="lineageResult.columnLineage?.length" :data="lineageResult.columnLineage" size="small">
            <el-table-column prop="targetTable" label="目标表" min-width="110" />
            <el-table-column prop="targetColumn" label="目标列" min-width="100" />
            <el-table-column label="" width="24"><template #default>←</template></el-table-column>
            <el-table-column prop="sourceColumn" label="源列" min-width="100" />
          </el-table>
          <el-empty v-else description="未解析到列级血缘" :image-size="50" />
        </template>
      </template>
      <el-empty v-else description="输入 SQL 后点击血缘解析" :image-size="50" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { datasourceApi } from '@/api/datasource'
import { queryApi, type QueryResult } from '@/api/query'
import { workflowApi, type SqlParseRes } from '@/api/workflow'

const SAMPLE_SQL = `-- MYSQL 兼容示例：选择 demo-mysql(ds-test1) 后可直接执行
SELECT VERSION() AS version, NOW() AS now_time, 1 + 1 AS sum_value;`

const editorEl = ref<HTMLDivElement>()
const editor = shallowRef<any>(null)
const monacoRef = shallowRef<any>(null)

const sql = ref(SAMPLE_SQL)
const datasource = ref('')
const datasources = ref<{ id: string; name: string }[]>([])
const running = ref(false)
const result = ref<{ columns: string[]; rows: Record<string, unknown>[]; elapsed: string } | null>(null)
const lastError = ref('')

const historyVisible = ref(false)
const historyLoading = ref(false)
const historyRows = ref<QueryResult[]>([])

const lineageVisible = ref(false)
const lineageLoading = ref(false)
const lineageResult = ref<SqlParseRes | null>(null)

async function openHistory() {
  historyVisible.value = true
  historyLoading.value = true
  try {
    const res = await queryApi.history({ page: 0, size: 30 })
    historyRows.value = res.content || []
  } finally {
    historyLoading.value = false
  }
}

function applyHistory(h: QueryResult) {
  sql.value = h.sql
  const ds = datasources.value.find((d) => d.name === h.datasourceName)
  if (ds) datasource.value = ds.name
  editor.value?.setValue(h.sql)
  historyVisible.value = false
}

async function loadDatasources() {
  try {
    const list = await datasourceApi.list()
    if (list?.length) {
      datasources.value = list.map((d) => ({ id: d.id, name: d.name }))
      datasource.value = datasources.value[0].name
    } else {
      datasources.value = [{ id: '', name: 'Hive (数仓)' }]
    }
  } catch {
    datasources.value = [{ id: '', name: 'Hive (数仓)' }]
  }
}

async function initMonaco() {
  // monaco-editor has NO default export (named namespace exports only); using
  // `.default` yields `undefined` and crashes editor.create → blank editor.
  const monaco = await import('monaco-editor')
  monacoRef.value = monaco
  editor.value = monaco.editor.create(editorEl.value!, {
    value: sql.value,
    language: 'sql',
    theme: 'vs-dark',
    fontFamily: 'JetBrains Mono, monospace',
    fontSize: 12,
    lineNumbers: 'on',
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    automaticLayout: true,
    tabSize: 2,
  })
  editor.value.onDidChangeModelContent(() => {
    sql.value = editor.value.getValue()
  })
  // Ctrl/Cmd+Enter to execute
  editor.value.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, execute)
}

function formatSql() {
  if (!editor.value) return
  const monaco = monacoRef.value
  editor.value.getAction('editor.action.formatDocument')?.run()
  void monaco
}

async function parseLineage() {
  if (!sql.value.trim()) { ElMessage.warning('请先输入 SQL'); return }
  lineageLoading.value = true
  try {
    const res = await workflowApi.sqlLineageParse(sql.value, 'MYSQL')
    lineageResult.value = res
    lineageVisible.value = true
    ElMessage[res.success ? 'success' : 'error'](res.success ? '血缘解析完成' : (res.message || '解析失败'))
  } catch (err: any) {
    ElMessage.error(err?.message || '血缘解析失败')
  } finally {
    lineageLoading.value = false
  }
}

async function execute() {
  if (!sql.value.trim()) return
  const ds = datasources.value.find((d) => d.name === datasource.value)
  if (!ds || !ds.id) {
    ElMessage.warning('请先选择有效数据源（当前仅演示数据源可执行）')
    return
  }
  running.value = true
  result.value = null
  lastError.value = ''
  try {
    const res: QueryResult = await queryApi.execute(sql.value, ds.id, ds.name)
    if (res.status === 'FAILED') {
      lastError.value = res.errorMsg || '执行失败'
      ElMessage.error(lastError.value)
      return
    }
    result.value = {
      columns: res.columns || [],
      rows: res.rows || [],
      elapsed: `${(res.durationMs / 1000).toFixed(2)}s`,
    }
    if (!result.value.rows.length) ElMessage.success('执行成功（0 行）')
  } catch (err: any) {
    lastError.value = err?.message || '执行失败'
    ElMessage.error(lastError.value)
  } finally {
    running.value = false
  }
}

function exportCsv() {
  if (!result.value) return
  const { columns, rows } = result.value
  const header = columns.join(',')
  const body = rows.map((r) => columns.map((c) => r[c] ?? '').join(',')).join('\n')
  const blob = new Blob([`\ufeff${header}\n${body}`], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = 'query_result.csv'
  a.click()
  URL.revokeObjectURL(a.href)
}

onMounted(async () => {
  await loadDatasources()
  await initMonaco()
})

onBeforeUnmount(() => {
  editor.value?.dispose()
})
</script>

<style scoped>
.sql-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--td-bg);
}
.toolbar {
  background: var(--td-surface);
  border-bottom: 1px solid var(--td-border);
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.flex-1 {
  flex: 1;
}
.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--td-text-3);
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.dot.ok {
  background: var(--td-success);
}
.main {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}
.editor-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--td-border);
  min-width: 0;
}
.editor-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #1e293b;
  flex-shrink: 0;
}
.traffic {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #dc2626;
}
.traffic.amber {
  background: #f59e0b;
}
.traffic.green {
  background: #10b981;
}
.file-name {
  margin-left: 6px;
  font-size: 10px;
  color: #64748b;
}
.monaco-host {
  flex: 1;
  min-height: 0;
}
.result-pane {
  width: 48%;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.result-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  background: var(--td-border-light);
  border-bottom: 1px solid var(--td-border);
  flex-shrink: 0;
}
.result-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--td-text-2);
}
.result-sub {
  font-size: 10px;
  color: var(--td-text-4);
}
.result-body {
  flex: 1;
  overflow: auto;
}
.result-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--td-text-4);
  font-size: 13px;
}
.re-icon {
  margin-bottom: 10px;
  color: var(--td-border);
}
.mono {
  font-family: var(--td-font-mono);
  font-size: 12px;
}
.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.history-item {
  padding: 10px 12px;
  border: 1px solid var(--td-border-light);
  border-radius: 10px;
  background: var(--td-surface);
  cursor: pointer;
  transition: border-color 0.15s;
}
.history-item:hover {
  border-color: var(--td-primary);
}
.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.history-time {
  font-size: 11px;
  color: var(--td-text-4);
}
.history-sql {
  font-size: 12px;
  color: var(--td-text-1);
  white-space: pre-wrap;
  word-break: break-all;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.history-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 11px;
  color: var(--td-text-4);
}
.lg-row { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 12px; font-size: 13px; }
.lg-label { width: 90px; flex: none; color: var(--td-text-3); }
.lg-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.lg-tag { margin: 0; }
</style>
