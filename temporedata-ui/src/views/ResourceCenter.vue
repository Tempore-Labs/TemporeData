<template>
  <div class="resource-page">
    <div class="page-header">
      <h2 class="page-title">资源中心</h2>
      <el-button type="primary" :icon="Plus" @click="uploadDialogVisible = true">上传文件</el-button>
    </div>

    <div class="page-body">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="文件名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="originalName" label="原始名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="文件大小" width="120" align="right">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="文件类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="light">{{ row.fileType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button type="primary" link :icon="Download" @click="handleDownload(row)">下载</el-button>
              <el-button v-if="row.packaged" type="primary" link :icon="FolderOpened" @click="openMembers(row)">成员</el-button>
              <el-button v-if="row.imageOptimized" type="warning" link :icon="Picture" @click="handleDownloadOriginal(row)">原图</el-button>
              <el-popconfirm title="确定要删除该文件吗？" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button type="danger" link :icon="Delete">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="handlePageSizeChange"
          @current-change="fetchData"
        />
      </div>
    </div>

    <!-- 上传文件弹窗 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传文件"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">
          <p>将文件拖到此处，或 <em>点击上传</em></p>
          <p class="upload-hint">支持 jar、zip、tar.gz 等格式</p>
        </div>
      </el-upload>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" :disabled="!uploadFile" @click="handleUpload">确定上传</el-button>
      </template>
    </el-dialog>

    <!-- 打包成员弹窗 -->
    <el-dialog v-model="membersVisible" title="打包成员" width="480px">
      <el-table :data="membersList" stripe size="small" max-height="320">
        <el-table-column prop="name" label="成员文件" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Download" @click="downloadMember(row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Delete, Download, UploadFilled, FolderOpened, Picture } from '@element-plus/icons-vue'
import { fileApi } from '@/api/modules/file'
import { ElMessage } from 'element-plus'

// ---- 表格数据 ----
const tableData = ref([])
const loading = ref(false)
const total = ref(0)

// ---- 分页 ----
const currentPage = ref(1)
const pageSize = ref(10)

// ---- 上传弹窗 ----
const uploadDialogVisible = ref(false)
const uploadLoading = ref(false)
const uploadRef = ref(null)
const uploadFile = ref(null)

// ---- 打包成员弹窗 ----
const membersVisible = ref(false)
const membersList = ref([])
let memberFileId = null

function handleFileChange(file) {
  uploadFile.value = file.raw
}

function handleFileRemove() {
  uploadFile.value = null
}

async function openMembers(row) {
  try {
    const names = await fileApi.members(row.id)
    membersList.value = (names || []).map(name => ({ name }))
    memberFileId = row.id
    membersVisible.value = true
  } catch (err) {
    ElMessage.error(err.message || '加载成员失败')
  }
}

function downloadMember(row) {
  if (!memberFileId) return
  window.open(fileApi.getMemberUrl(memberFileId, row.name), '_blank')
}

// ---- 工具函数 ----
function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

// ---- 加载数据 ----
async function fetchData() {
  loading.value = true
  try {
    const res = await fileApi.list({ page: currentPage.value, size: pageSize.value, bizType: 'RESOURCE' })
    tableData.value = res.items || []
    total.value = res.total || 0
  } catch (err) {
    ElMessage.error(err.message || '加载文件列表失败')
  } finally {
    loading.value = false
  }
}

function handlePageSizeChange() {
  currentPage.value = 1
  fetchData()
}

// ---- 上传 ----
async function handleUpload() {
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  uploadLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.value)
    formData.append('bizType', 'RESOURCE')
    await fileApi.upload(formData)
    ElMessage.success('文件上传成功')
    uploadDialogVisible.value = false
    uploadFile.value = null
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '上传失败')
  } finally {
    uploadLoading.value = false
  }
}

// ---- 下载 ----
function handleDownload(row) {
  const url = fileApi.getDownloadUrl(row.id)
  window.open(url, '_blank')
}

function handleDownloadOriginal(row) {
  window.open(fileApi.getOriginalUrl(row.id), '_blank')
}

// ---- 删除 ----
async function handleDelete(id) {
  try {
    await fileApi.delete(id)
    ElMessage.success('文件已删除')
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
.resource-page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.page-body {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.upload-area {
  width: 100%;
}

.upload-icon {
  font-size: 48px;
  color: var(--spark-text-muted);
  margin-bottom: 8px;
}

.upload-text {
  color: var(--spark-text-secondary);
  font-size: 14px;
}

.upload-text em {
  color: var(--spark-primary);
  font-style: normal;
  cursor: pointer;
}

.upload-hint {
  font-size: 12px;
  color: var(--spark-text-muted);
  margin-top: 4px;
}
</style>