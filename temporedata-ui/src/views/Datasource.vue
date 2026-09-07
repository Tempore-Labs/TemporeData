<template>
  <SearchTable
    title="数据源管理"
    :loading="crud.table.loading"
    :total="crud.table.total"
    v-model:current-page="crud.table.currentPage"
    v-model:page-size="crud.table.pageSize"
    add-text="新增数据源"
    @add="crud.dialog.openAdd"
  >
    <template #header-extra>
      <el-button @click="plugin.visible = true">
        <el-icon><Setting /></el-icon>
        驱动插件
      </el-button>
    </template>

    <!-- Table -->
    <template #default>
      <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe style="width: 100%">
        <el-table-column prop="name" label="数据源名称" min-width="160" />
        <el-table-column prop="type" label="类型" width="130">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" effect="light">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="主机地址" min-width="160" />
        <el-table-column prop="port" label="端口" width="90" align="center" />
        <el-table-column label="状态" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'CONNECTED' ? 'success' : 'danger'" effect="dark">
              <el-icon style="margin-right: 4px; vertical-align: middle;"><Connection /></el-icon>
              {{ row.status === 'CONNECTED' ? '已连接' : '已断开' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="crud.dialog.openEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button size="small" type="warning" link @click="handleTest(row)">
              <el-icon><Connection /></el-icon>测试
            </el-button>
            <el-popconfirm title="确定删除该数据源吗？" @confirm="crud.handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link>
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- Datasource Dialog -->
    <template #dialog>
      <el-dialog
        v-model="crud.dialog.visible"
        :title="crud.dialog.isEdit ? '编辑数据源' : '新增数据源'"
        width="520px"
        :close-on-click-modal="false"
        destroy-on-close
      >
        <el-form ref="crud.dialog.formRef" :model="crud.dialog.form" :rules="crud.dialog.rules" label-width="100px" label-position="right">
          <el-form-item label="数据源名称" prop="name">
            <el-input v-model="crud.dialog.form.name" placeholder="请输入数据源名称" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="类型" prop="type">
            <el-select v-model="crud.dialog.form.type" placeholder="请选择数据源类型" style="width: 100%">
              <el-option label="MySQL" value="MySQL" />
              <el-option label="PostgreSQL" value="PostgreSQL" />
              <el-option label="ClickHouse" value="ClickHouse" />
              <el-option label="Oracle" value="Oracle" />
              <el-option label="Doris" value="Doris" />
              <el-option label="StarRocks" value="StarRocks" />
              <el-option label="Hive" value="Hive" />
              <el-option label="OceanBase" value="OceanBase" />
            </el-select>
          </el-form-item>
          <el-form-item label="主机地址" prop="host">
            <el-input v-model="crud.dialog.form.host" placeholder="请输入主机地址" />
          </el-form-item>
          <el-form-item label="端口" prop="port">
            <el-input-number v-model="crud.dialog.form.port" :min="1" :max="65535" placeholder="请输入端口" style="width: 100%" />
          </el-form-item>
          <el-form-item label="数据库" prop="database">
            <el-input v-model="crud.dialog.form.database" placeholder="请输入数据库名" />
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="crud.dialog.form.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="crud.dialog.form.password" type="password" show-password placeholder="请输入密码" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="crud.dialog.close()">取消</el-button>
          <el-button type="primary" :loading="crud.dialog.submitting" @click="crud.handleSubmit">确定</el-button>
        </template>
      </el-dialog>
    </template>
  </SearchTable>

  <!-- Supported datasource plugin list (并入数据源管理，只读) -->
  <el-dialog v-model="plugin.visible" title="数据源插件（已支持）" width="680px" :close-on-click-modal="false">
    <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px; flex-wrap: wrap;">
      <el-input
        v-model="plugin.keyword"
        placeholder="搜索类型 / 名称"
        clearable
        style="width: 220px"
      ></el-input>
      <el-select v-model="plugin.source" style="width: 130px">
        <el-option label="全部来源" value="ALL" />
        <el-option label="内置" value="BUILTIN" />
        <el-option label="上传" value="UPLOAD" />
      </el-select>
      <div style="flex:1"></div>
      <el-button type="primary" @click="plugin.addDialog = true">
        <el-icon><Plus /></el-icon>新增插件
      </el-button>
    </div>
    <el-table :data="filteredPlugins" v-loading="plugin.loading" stripe style="width: 100%">
      <el-table-column prop="type" label="类型" width="150">
        <template #default="{ row }">
          <el-tag effect="light">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="120" />
      <el-table-column prop="defaultPort" label="默认端口" width="110" align="center" />
      <el-table-column label="来源" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.builtin ? 'info' : 'success'" effect="plain">
            {{ row.builtin ? '内置' : '上传' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="plugin.addDialog" title="新增数据源插件" width="560px" append-to-body :close-on-click-modal="false">
      <el-form :model="plugin.form" label-width="110px" label-position="right">
        <el-form-item label="插件名称" required>
          <el-input v-model="plugin.form.name" placeholder="请输入插件名称" />
        </el-form-item>
        <el-form-item label="Maven Group">
          <el-input v-model="plugin.form.mvnGroup" placeholder="驱动 GAV groupId，如 com.mysql" />
        </el-form-item>
        <el-form-item label="Maven Artifact">
          <el-input v-model="plugin.form.mvnArtifact" placeholder="驱动 GAV artifactId，如 mysql-connector-j" />
        </el-form-item>
        <el-form-item label="Maven Version">
          <el-input v-model="plugin.form.mvnVersion" placeholder="驱动 GAV version，如 8.0.33" />
        </el-form-item>
        <el-divider content-position="left">或指定本地 jar 路径</el-divider>
        <el-form-item label="驱动路径">
          <el-input v-model="plugin.form.driverPath" placeholder="（可选）服务端驱动 jar 路径" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="plugin.addDialog = false">取消</el-button>
        <el-button type="primary" :loading="plugin.submitting" @click="handlePluginCreate">确定</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { datasourceApi } from '@/api/modules/datasource'
import { datasourcePluginApi } from '@/api/modules/datasourcePlugin'
import { useCRUD } from '@/composables/useCRUD'
import { ElMessage } from 'element-plus'
import { Connection, Edit, Delete, Setting, Plus } from '@element-plus/icons-vue'
import SearchTable from '@/components/SearchTable.vue'

const defaultForm = () => ({
  name: '',
  type: '',
  host: '',
  port: 3306,
  database: '',
  username: '',
  password: ''
})

const rules = {
  name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  database: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const crud = useCRUD(datasourceApi, defaultForm, rules, { entityName: '数据源' })

// Custom action: test connection
async function handleTest(row) {
  try {
    const result = await datasourceApi.test(row.id)
    ElMessage.success(result?.msg || '连接测试成功')
  } catch (err) {
    ElMessage.error(err.message || '连接测试失败')
  }
}

function typeTag(type) {
  const map = { MySQL: '', PostgreSQL: 'success', Hive: 'warning', HDFS: 'danger' }
  return map[type] || 'info'
}

// ---- Supported datasource plugin list (并入数据源管理，只读 + GAV 上传) ----
const plugin = reactive({
  visible: false,
  addDialog: false,
  loading: false,
  submitting: false,
  list: [],
  keyword: '',
  source: 'ALL',
  form: { name: '', mvnGroup: '', mvnArtifact: '', mvnVersion: '', driverPath: '' }
})

// Filter list by keyword (type/name) and source (builtin/upload)
const filteredPlugins = computed(() => {
  const kw = plugin.keyword.trim().toLowerCase()
  return plugin.list.filter(p => {
    const matchKw = !kw
      || p.type.toLowerCase().includes(kw)
      || (p.name || '').toLowerCase().includes(kw)
    const matchSrc = plugin.source === 'ALL'
      || (plugin.source === 'BUILTIN' ? !!p.builtin : !p.builtin)
    return matchKw && matchSrc
  })
})

async function loadPlugins() {
  plugin.loading = true
  try {
    const res = await datasourcePluginApi.list()
    plugin.list = res?.data || res || []
  } catch (err) {
    ElMessage.error(err.message || '加载插件失败')
  } finally {
    plugin.loading = false
  }
}

async function handlePluginCreate() {
  const f = plugin.form
  const hasGav = f.mvnGroup && f.mvnArtifact && f.mvnVersion
  const hasPath = f.driverPath
  if (!f.name) { ElMessage.error('请输入插件名称'); return }
  if (!hasGav && !hasPath) { ElMessage.error('请填写 Maven 坐标(GAV) 或本地驱动路径'); return }
  plugin.submitting = true
  try {
    const res = await datasourcePluginApi.create({
      name: f.name,
      mvnGroup: f.mvnGroup || undefined,
      mvnArtifact: f.mvnArtifact || undefined,
      mvnVersion: f.mvnVersion || undefined,
      driverPath: f.driverPath || undefined
    })
    ElMessage.success(res?.msg || '插件加载成功')
    plugin.addDialog = false
    plugin.form = { name: '', mvnGroup: '', mvnArtifact: '', mvnVersion: '', driverPath: '' }
    await loadPlugins()
  } catch (err) {
    ElMessage.error(err.message || '新增失败')
  } finally {
    plugin.submitting = false
  }
}

onMounted(() => {
  crud.table.fetch()
  loadPlugins()
})
</script>