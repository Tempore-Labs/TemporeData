<template>
  <div class="container-page">
    <div class="page-header">
      <h2 class="page-title">容器管理</h2>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">创建容器</el-button>
    </div>

    <el-table :data="containers" v-loading="loading" stripe>
      <el-table-column prop="name" label="容器名称" min-width="160" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 'DOCKER' ? '' : 'success'" effect="light">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="image" label="镜像" min-width="180" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="containerStatusTag(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="cpuCores" label="CPU核" width="80" align="center" />
      <el-table-column prop="memoryMb" label="内存(MB)" width="100" align="right" />
      <el-table-column prop="createDateTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <div class="action-btns">
            <el-button v-if="row.status !== 'RUNNING'" type="success" link :icon="VideoPlay" @click="handleStart(row)">启动</el-button>
            <el-button v-else type="warning" link :icon="VideoPause" @click="handleStop(row)">停止</el-button>
            <el-button type="info" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该容器吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑容器' : '创建容器'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="容器名称" prop="name">
          <el-input v-model="form.name" placeholder="容器名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="Docker" value="DOCKER" />
            <el-option label="Kubernetes" value="K8S" />
          </el-select>
        </el-form-item>
        <el-form-item label="镜像" prop="image">
          <el-input v-model="form.image" placeholder="例如: spark:3.4.0" />
        </el-form-item>
        <el-form-item label="CPU核数" prop="cpuCores">
          <el-input-number v-model="form.cpuCores" :min="1" :max="64" />
        </el-form-item>
        <el-form-item label="内存(MB)" prop="memoryMb">
          <el-input-number v-model="form.memoryMb" :min="128" :max="65536" :step="128" />
        </el-form-item>
        <el-form-item label="所属集群" prop="clusterId">
          <el-select v-model="form.clusterId" placeholder="选择集群" clearable style="width: 100%">
            <el-option v-for="c in clusters" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="启动命令" prop="command">
          <el-input v-model="form.command" placeholder="容器启动命令" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Edit, Delete, VideoPlay, VideoPause } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { containerApi } from '@/api/modules/container'
import { clusterApi } from '@/api/modules/cluster'

const loading = ref(false)
const containers = ref([])
const clusters = ref([])

// Dialog
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const editingId = ref('')
const form = ref({ name: '', type: 'DOCKER', image: '', cpuCores: 2, memoryMb: 2048, clusterId: '', command: '' })
const rules = {
  name: [{ required: true, message: '请输入容器名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  image: [{ required: true, message: '请输入镜像', trigger: 'blur' }]
}

onMounted(() => {
  fetchContainers()
  fetchClusters()
})

async function fetchContainers() {
  loading.value = true
  try {
    containers.value = await containerApi.list() || []
  } catch (e) { ElMessage.error('获取容器列表失败') } finally { loading.value = false }
}

async function fetchClusters() {
  try { clusters.value = await clusterApi.list() || [] } catch (e) { /* ignore */ }
}

function openAddDialog() {
  isEdit.value = false
  editingId.value = ''
  form.value = { name: '', type: 'DOCKER', image: '', cpuCores: 2, memoryMb: 2048, clusterId: '', command: '' }
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editingId.value = row.id
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (isEdit.value) {
      await containerApi.update(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await containerApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchContainers()
  } catch (e) { ElMessage.error('操作失败') }
}

async function handleStart(row) {
  try {
    await containerApi.start(row.id)
    ElMessage.success('容器已启动')
    fetchContainers()
  } catch (e) { ElMessage.error('启动失败') }
}

async function handleStop(row) {
  try {
    await containerApi.stop(row.id)
    ElMessage.success('容器已停止')
    fetchContainers()
  } catch (e) { ElMessage.error('停止失败') }
}

async function handleDelete(id) {
  try {
    await containerApi.delete(id)
    ElMessage.success('删除成功')
    fetchContainers()
  } catch (e) { ElMessage.error('删除失败') }
}

function containerStatusTag(status) {
  const map = { CREATED: 'info', RUNNING: 'success', STOPPED: 'warning', FAILED: 'danger' }
  return map[status] || 'info'
}
</script>

<style scoped>
.container-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.action-btns { display: flex; gap: 2px; flex-wrap: wrap; }
</style>