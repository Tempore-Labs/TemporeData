<template>
  <div class="page">
    <PageHeader title="元数据采集" subtitle="元数据采集、表结构同步与变更监测">
      <template #actions>
        <el-button size="small" type="primary" :loading="collectingAll" @click="collectAll">采集全部</el-button>
        <el-button size="small" type="warning" @click="collectDialog = true">手动采集</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" class="meta-tabs">
      <el-tab-pane label="数据表" name="tables">
        <el-skeleton v-if="tablesLoading" :rows="6" animated />
        <DataTable v-else :data="tables">
          <el-table-column prop="schemaName" label="Schema" width="120">
            <template #default="{ row }"><span class="mono">{{ row.schemaName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="tableName" label="表名" min-width="200">
            <template #default="{ row }"><span class="mono">{{ row.tableName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="tableComment" label="注释" min-width="240" show-overflow-tooltip />
          <el-table-column prop="rowCount" label="行数" width="110" align="right">
            <template #default="{ row }"><span class="mono">{{ row.rowCount ?? '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="dataLevelCode" label="级别" width="90">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ row.dataLevelCode || '—' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastSyncTime" label="最近同步" min-width="150">
            <template #default="{ row }"><span class="mono">{{ row.lastSyncTime || '—' }}</span></template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>

      <el-tab-pane label="字段" name="columns">
        <DataTable :data="columns">
          <el-table-column prop="tableName" label="表名" min-width="160">
            <template #default="{ row }"><span class="mono">{{ row.tableName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="columnName" label="字段名" min-width="160">
            <template #default="{ row }"><span class="mono">{{ row.columnName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="columnType" label="类型" width="120">
            <template #default="{ row }"><span class="mono">{{ row.columnType || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="主键" width="80">
            <template #default="{ row }">{{ row.primaryKey ? '是' : '—' }}</template>
          </el-table-column>
          <el-table-column label="敏感" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.sensitiveFlag ? 'danger' : 'info'" effect="plain">{{ row.sensitiveFlag ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="comment" label="注释" min-width="220" show-overflow-tooltip />
        </DataTable>
      </el-tab-pane>

      <el-tab-pane label="变更记录" name="changes">
        <DataTable :data="changes">
          <el-table-column prop="tableName" label="表名" min-width="180">
            <template #default="{ row }"><span class="mono">{{ row.tableName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="changeType" label="变更类型" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="changeTypeTag(row.changeType)" effect="plain">{{ row.changeType || '—' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="checksum" label="Checksum" min-width="120">
            <template #default="{ row }"><span class="mono">{{ row.checksum || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="syncedAt" label="同步时间" min-width="150">
            <template #default="{ row }"><span class="mono">{{ row.syncedAt || '—' }}</span></template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>
    </el-tabs>

    <!-- manual collect dialog -->
    <el-dialog v-model="collectDialog" title="手动元数据采集" width="480px">
      <el-form :model="collectForm" label-width="90px">
        <el-form-item label="数据库" required>
          <el-input v-model="collectForm.database" placeholder="database name" />
        </el-form-item>
        <el-form-item label="主机">
          <el-input v-model="collectForm.host" placeholder="localhost" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input v-model.number="collectForm.port" placeholder="3306" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="collectForm.username" placeholder="root" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="collectForm.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="collectDialog = false">取消</el-button>
        <el-button type="primary" :loading="collecting" @click="doCollect">采集</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import { metaApi, type MetaTable, type MetaColumn, type MetaChange } from '@/api/ops2.module'

const tab = ref('tables')
const tables = ref<MetaTable[]>([])
const columns = ref<MetaColumn[]>([])
const changes = ref<MetaChange[]>([])
const tablesLoading = ref(false)
const collecting = ref(false)
const collectingAll = ref(false)
const collectDialog = ref(false)
const collectForm = ref({
  database: '',
  host: 'localhost',
  port: 3306,
  username: 'root',
  password: '',
})

async function loadTables() {
  tablesLoading.value = true
  try {
    tables.value = (await metaApi.tables()) || []
  } finally {
    tablesLoading.value = false
  }
}
async function loadColumns() {
  columns.value = (await metaApi.columns()) || []
}
async function loadChanges() {
  changes.value = (await metaApi.changes()) || []
}
loadTables()
watch(tab, (t) => {
  if (t === 'columns') loadColumns()
  else if (t === 'changes') loadChanges()
})

async function collectAll() {
  collectingAll.value = true
  try {
    const res = await metaApi.collectAll()
    ElMessage.success(`采集完成，处理 ${(res || []).length} 个数据源`)
    await loadTables()
  } finally {
    collectingAll.value = false
  }
}
async function doCollect() {
  if (!collectForm.value.database) {
    ElMessage.warning('数据库名必填')
    return
  }
  collecting.value = true
  try {
    await metaApi.collect({ ...collectForm.value })
    ElMessage.success('采集完成')
    collectDialog.value = false
    await loadTables()
  } finally {
    collecting.value = false
  }
}
function changeTypeTag(t?: string): 'warning' | 'danger' | 'info' | 'success' | 'primary' {
  switch (t) {
    case 'ADD':
      return 'success'
    case 'DROP':
      return 'danger'
    case 'TYPE_CHANGE':
      return 'warning'
    default:
      return 'info'
  }
}
</script>

<style scoped>
.page { padding: 24px; }
.meta-tabs { margin-top: 4px; }
.mono { font-family: var(--td-font-mono); }
</style>