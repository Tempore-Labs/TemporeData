<template>
  <div class="page">
    <PageHeader title="计算引擎" subtitle="Spark / Flink 计算引擎管理">
      <template #actions>
        <el-radio-group v-model="kind" size="small" style="margin-right: 12px">
          <el-radio-button value="SPARK">Spark</el-radio-button>
          <el-radio-button value="FLINK">Flink</el-radio-button>
        </el-radio-group>
        <el-button size="small" type="primary" @click="openCreate">+ 新建引擎</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="name" label="引擎名称" min-width="180">
        <template #default="{ row }"><span class="mono">{{ row.name || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 'SPARK' ? 'warning' : 'success'" effect="plain">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="mainClass" label="主类" min-width="200" show-overflow-tooltip>
        <template #default="{ row }"><span class="mono">{{ row.mainClass || row.pythonFile || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="parallelism" label="并行度" width="100" align="right">
        <template #default="{ row }"><span class="mono">{{ row.parallelism || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
      </el-table-column>
      <el-table-column prop="jobId" label="Job ID" min-width="160" show-overflow-tooltip>
        <template #default="{ row }"><span class="mono">{{ row.jobId || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑引擎' : `新建${kind}引擎`" width="600px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item v-if="kind === 'SPARK'" label="主类">
          <el-input v-model="form.mainClass" placeholder="com.example.Main" />
        </el-form-item>
        <el-form-item v-if="kind === 'SPARK'" label="JAR 路径">
          <el-input v-model="form.jarPath" />
        </el-form-item>
        <el-form-item v-if="kind === 'SPARK'" label="Python 文件">
          <el-input v-model="form.pythonFile" />
        </el-form-item>
        <el-form-item v-if="kind === 'FLINK'" label="SQL 内容">
          <el-input v-model="form.sqlContent" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="并行度">
          <el-input v-model="form.parallelism" />
        </el-form-item>
        <el-form-item label="部署模式">
          <el-select v-model="form.deployMode" style="width: 100%">
            <el-option v-for="s in ['yarn', 'k8s', 'local', 'standalone']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="Driver 内存">
          <el-input v-model="form.driverMemory" placeholder="如 2g" />
        </el-form-item>
        <el-form-item label="Executor 内存">
          <el-input v-model="form.executorMemory" placeholder="如 4g" />
        </el-form-item>
        <el-form-item label="Executor 核数">
          <el-input v-model.number="form.executorCores" />
        </el-form-item>
        <el-form-item label="Executor 数量">
          <el-input v-model.number="form.numExecutors" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ['READY', 'DEPLOYING', 'RUNNING', 'FAILED', 'STOPPED']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { engineApi, type EngineEntity } from '@/api/ops.module'

const queryClient = useQueryClient()
const kind = ref<'SPARK' | 'FLINK'>('SPARK')
const rows = ref<EngineEntity[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<EngineEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<EngineEntity>>({})

async function load() {
  loading.value = true
  try {
    rows.value = (kind.value === 'SPARK' ? await engineApi.sparkList() : await engineApi.flinkList()) || []
  } finally {
    loading.value = false
  }
}
load()
watch(kind, load)

function openCreate() {
  editing.value = null
  form.value = { name: '', status: 'READY', deployMode: 'yarn' }
  dialogVisible.value = true
}
function openEdit(row: EngineEntity) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.name) {
    ElMessage.warning('名称必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      if (kind.value === 'SPARK') await engineApi.sparkUpdate(editing.value.id, form.value)
      else await engineApi.flinkUpdate(editing.value.id, form.value)
    } else {
      if (kind.value === 'SPARK') await engineApi.sparkCreate(form.value)
      else await engineApi.flinkCreate(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['engine'] })
    await load()
  } finally {
    saving.value = false
  }
}
async function remove(row: EngineEntity) {
  await ElMessageBox.confirm(`确认删除引擎 ${row.name} ？`, '提示', { type: 'warning' })
  await engineApi.remove(row.id, row.type || kind.value)
  ElMessage.success('已删除')
  await load()
}
</script>

<style scoped>
.page { padding: 24px; }
.mono { font-family: var(--td-font-mono); }
</style>