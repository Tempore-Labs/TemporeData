<template>
  <div class="page">
    <PageHeader title="接口服务" subtitle="数据服务 API 管理与调用日志">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新建 API</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- API 管理 -->
      <el-tab-pane label="API 管理" name="apis">
        <el-skeleton v-if="apiLoading" :rows="6" animated />
        <DataTable v-else :data="apis">
          <el-table-column prop="name" label="API 名称" min-width="150">
            <template #default="{ row }"><span class="mono">{{ row.name }}</span></template>
          </el-table-column>
          <el-table-column prop="method" label="方法" width="90">
            <template #default="{ row }"><el-tag size="small" :type="row.method === 'POST' ? 'warning' : 'success'">{{ row.method || 'GET' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="path" label="路径" min-width="180">
            <template #default="{ row }"><span class="mono">{{ row.path || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <StatusBadge :status="row.status" :text="row.status" />
            </template>
          </el-table-column>
          <el-table-column prop="apiKey" label="API Key" min-width="120">
            <template #default="{ row }">
              <el-tooltip :content="row.apiKey || ''" placement="top">
                <span class="mono muted">{{ row.apiKey ? (row.apiKey.slice(0, 6) + '…') : '—' }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="test(row)">测试</el-button>
              <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text type="primary" @click="toggle(row)">{{ row.status === 'ENABLED' ? '停用' : '启用' }}</el-button>
              <el-button size="small" text type="warning" @click="regenerate(row)">重置Key</el-button>
              <el-button size="small" text type="danger" @click="delApi(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>

      <!-- 调用日志 -->
      <el-tab-pane label="调用日志" name="logs">
        <el-skeleton v-if="logLoading" :rows="6" animated />
        <DataTable v-else :data="logs">
          <el-table-column prop="apiName" label="API 名称" min-width="140">
            <template #default="{ row }"><span class="mono">{{ row.apiName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="method" label="方法" width="90">
            <template #default="{ row }"><el-tag size="small">{{ row.method || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="requestIp" label="来源 IP" width="150">
            <template #default="{ row }"><span class="mono">{{ row.requestIp || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="响应码" width="100">
            <template #default="{ row }">
              <StatusBadge :status="(row.responseCode ?? 0) < 400 ? 'success' : 'failed'" :text="String(row.responseCode ?? '—')" />
            </template>
          </el-table-column>
          <el-table-column prop="costTime" label="耗时(ms)" width="110">
            <template #default="{ row }"><span class="mono">{{ row.costTime ?? '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="createTime" label="调用时间" min-width="160">
            <template #default="{ row }"><span class="mono muted">{{ row.createTime || '—' }}</span></template>
          </el-table-column>
        </DataTable>
        <el-pagination
          v-if="logTotal > 10"
          class="pager"
          layout="total, prev, pager, next"
          :total="logTotal"
          :page-size="10"
          :current-page="logPage"
          @current-change="(p: number) => { logPage = p; loadLogs() }"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- create/edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑 API' : '新建 API'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="HTTP 方法">
          <el-select v-model="form.method" style="width: 100%">
            <el-option v-for="m in ['GET', 'POST']" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径" required>
          <el-input v-model="form.path" placeholder="/api/v1/orders" />
        </el-form-item>
        <el-form-item label="数据源">
          <el-input v-model="form.datasourceId" />
        </el-form-item>
        <el-form-item label="SQL 语句">
          <el-input v-model="form.sql" type="textarea" :rows="4" />
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
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { apiLogApi, dataServiceApi, type ApiLogEntity, type DataApiEntity } from '@/api/dataService'

const queryClient = useQueryClient()
const tab = ref('apis')

const apis = ref<DataApiEntity[]>([])
const apiLoading = ref(false)

const logs = ref<ApiLogEntity[]>([])
const logLoading = ref(false)
const logPage = ref(1)
const logTotal = ref(0)

const dialogVisible = ref(false)
const editing = ref<DataApiEntity | null>(null)
const saving = ref(false)
const form = ref<Partial<DataApiEntity>>({})

async function loadApis() {
  apiLoading.value = true
  try {
    apis.value = await dataServiceApi.list()
  } finally {
    apiLoading.value = false
  }
}
async function loadLogs() {
  logLoading.value = true
  try {
    const res = await apiLogApi.page({ page: logPage.value - 1, size: 10 })
    logs.value = res.content || []
    logTotal.value = res.totalElements || 0
  } finally {
    logLoading.value = false
  }
}
loadApis()

function onTabChange(name: string | number) {
  if (name === 'logs' && logs.value.length === 0) loadLogs()
}

function openCreate() {
  editing.value = null
  form.value = { name: '', method: 'GET', path: '', datasourceId: '', sql: '', description: '' }
  dialogVisible.value = true
}
function openEdit(row: DataApiEntity) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.name || !form.value.path) {
    ElMessage.warning('名称与路径必填')
    return
  }
  saving.value = true
  try {
    if (editing.value) await dataServiceApi.update(editing.value.id, form.value)
    else await dataServiceApi.create(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['dataService'] })
    await loadApis()
  } finally {
    saving.value = false
  }
}
async function test(row: DataApiEntity) {
  try {
    const res = await dataServiceApi.test(row.id)
    ElMessage.success(`测试成功: ${JSON.stringify(res)?.slice(0, 100) || 'ok'}`)
  } catch (err: any) {
    ElMessage.error(err?.message || '测试失败')
  }
}
async function toggle(row: DataApiEntity) {
  await dataServiceApi.toggle(row.id)
  ElMessage.success('状态已切换')
  await loadApis()
}
async function regenerate(row: DataApiEntity) {
  await ElMessageBox.confirm(`确认重置 API ${row.name} 的 Key？旧 Key 将失效`, '提示', { type: 'warning' })
  await dataServiceApi.regenerateKey(row.id)
  ElMessage.success('Key 已重置')
  await loadApis()
}
async function delApi(row: DataApiEntity) {
  await ElMessageBox.confirm(`确认删除 API ${row.name} ？`, '提示', { type: 'warning' })
  await dataServiceApi.remove(row.id)
  ElMessage.success('已删除')
  await loadApis()
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