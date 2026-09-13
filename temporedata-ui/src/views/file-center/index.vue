<template>
  <div class="page">
    <PageHeader title="文件中心" subtitle="文件上传、打包与下载管理">
      <template #actions>
        <el-upload
          :show-file-list="false"
          :http-request="doUpload"
          :before-upload="beforeUpload"
          accept=".jar,.zip,.tar.gz,.xlsx,.xls,.csv,.txt,.json,.py,.pdf,.png,.jpg"
        >
          <el-button size="small" type="primary">+ 上传文件</el-button>
        </el-upload>
      </template>
    </PageHeader>

    <div class="stat-row" v-if="stats && Object.keys(stats).length">
      <el-statistic v-for="(v, k) in stats" :key="k" :title="String(k)" :value="Number(v) || 0" />
    </div>

    <el-input v-model="keyword" placeholder="搜索文件名..." size="small" clearable style="max-width: 260px; margin-bottom: 12px" @change="reload" />

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="originalName" label="文件名" min-width="240">
        <template #default="{ row }"><span class="mono">{{ row.originalName || row.name || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小" width="120" align="right">
        <template #default="{ row }"><span class="mono">{{ formatSize(row.fileSize) }}</span></template>
      </el-table-column>
      <el-table-column prop="bizType" label="业务类型" width="120">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.bizType || '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ext" label="类型" width="90">
        <template #default="{ row }"><span class="mono">{{ row.ext || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" min-width="150">
        <template #default="{ row }"><span class="mono">{{ row.createTime || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="download(row)">下载</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'
import http from '@/api/http'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import { fileCenterApi, type FileItem } from '@/api/ops2.module'

const rows = ref<FileItem[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const keyword = ref('')
const stats = ref<Record<string, unknown>>({})

async function loadStats() {
  try {
    stats.value = (await fileCenterApi.stats()) || {}
  } catch {
    stats.value = {}
  }
}
async function reload() {
  await load()
}
async function load() {
  loading.value = true
  try {
    const res = await fileCenterApi.page({ page: page.value - 1, size, keyword: keyword.value || undefined })
    rows.value = res?.items || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}
load()
loadStats()

function onPage(p: number) {
  page.value = p
  load()
}
function formatSize(n?: number): string {
  if (n == null) return '—'
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(1)} MB`
}
function beforeUpload(file: File): boolean {
  if (file.size > 256 * 1024 * 1024) {
    ElMessage.warning('文件超过 256MB 限制')
    return false
  }
  return true
}
async function doUpload(options: UploadRequestOptions) {
  const fd = new FormData()
  fd.append('file', options.file)
  try {
    await http.post('/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    ElMessage.success('上传成功')
    await load()
    await loadStats()
  } catch {
    ElMessage.error('上传失败')
  }
}
async function download(row: FileItem) {
  const url = fileCenterApi.downloadUrl(row.id)
  const a = document.createElement('a')
  a.href = `/api${url.split('/api')[1] || url}`
  a.download = row.originalName || 'file'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}
async function remove(row: FileItem) {
  await ElMessageBox.confirm(`确认删除文件 ${row.originalName} ？`, '提示', { type: 'warning' })
  await fileCenterApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
  await loadStats()
}
</script>

<style scoped>
.page { padding: 24px; }
.stat-row { display: flex; gap: 48px; padding: 16px 0; }
.pager { margin-top: 16px; justify-content: flex-end; }
.mono { font-family: var(--td-font-mono); }
</style>