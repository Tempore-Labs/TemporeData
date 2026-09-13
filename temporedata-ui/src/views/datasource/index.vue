<template>
  <div class="page">
    <PageHeader title="数据源管理" subtitle="数据集成连接配置与连通性测试">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建数据源</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="name" label="名称" min-width="140">
        <template #default="{ row }"><span class="mono">{{ row.name }}</span></template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="110">
        <template #default="{ row }"><el-tag size="small">{{ row.type }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="host" label="主机" min-width="140">
        <template #default="{ row }"><span class="mono">{{ row.host || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="port" label="端口" width="80">
        <template #default="{ row }"><span class="mono">{{ row.port ?? '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="database" label="数据库" min-width="130">
        <template #default="{ row }"><span class="mono">{{ row.database || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" width="110">
        <template #default="{ row }"><span class="mono">{{ row.username || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusBadge :status="row.status || 'active'" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" :loading="testingId === row.id" @click="test(row)">测试</el-button>
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

    <!-- Create/edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑数据源' : '新建数据源'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如 demo-mysql" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="t in ['MYSQL', 'POSTGRESQL', 'HIVE', 'CLICKHOUSE', 'ORACLE', 'DORIS', 'STARROCKS', 'OCEANBASE', 'KAFKA']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机">
          <el-input v-model="form.host" placeholder="localhost" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="数据库">
          <el-input v-model="form.database" placeholder="库名" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="留空则不修改" />
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
import { datasourceApi, type DatasourceEntity } from '@/api/datasource'

const queryClient = useQueryClient()
const rows = ref<DatasourceEntity[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)

const dialogVisible = ref(false)
const editing = ref<DatasourceEntity | null>(null)
const saving = ref(false)
const testingId = ref<string>('')
const form = ref<Partial<DatasourceEntity>>({})

async function load() {
  loading.value = true
  try {
    const res = await datasourceApi.page({ page: page.value - 1, size })
    rows.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}
load()
watch(page, load)

function onPage(p: number) {
  page.value = p
}

function openCreate() {
  editing.value = null
  form.value = { name: '', type: 'MYSQL', host: 'localhost', port: 3306, database: '', username: 'root', password: '' }
  dialogVisible.value = true
}
function openEdit(row: DatasourceEntity) {
  editing.value = row
  form.value = { ...row, password: '' }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.name) {
    ElMessage.warning('名称必填')
    return
  }
  saving.value = true
  try {
    const payload = { ...form.value }
    if (!payload.password) delete payload.password
    if (editing.value) await datasourceApi.update(editing.value.id, payload)
    else await datasourceApi.create(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['datasource'] })
    await load()
  } finally {
    saving.value = false
  }
}

async function test(row: DatasourceEntity) {
  testingId.value = row.id
  try {
    const res = await datasourceApi.test(row.id)
    if (res?.ok) ElMessage.success('连接成功')
    else ElMessage.warning(res?.message || '连接失败')
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || '连接测试失败')
  } finally {
    testingId.value = ''
  }
}

async function remove(row: DatasourceEntity) {
  await ElMessageBox.confirm(`确认删除数据源 ${row.name} ？`, '提示', { type: 'warning' })
  await datasourceApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
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
