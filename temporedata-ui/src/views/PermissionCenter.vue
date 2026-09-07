<template>
  <div class="perm-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">权限中心</h2>
        <p class="page-desc">资源目录 + 角色-资源授权矩阵，后端强制执行（DENY 优先、通配 *）</p>
      </div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="资源目录" name="resources" />
      <el-tab-pane label="授权矩阵" name="permissions" />
    </el-tabs>

    <!-- 资源目录 -->
    <el-card v-if="tab === 'resources'" shadow="never">
      <div class="toolbar">
        <el-select v-model="typeFilter" placeholder="资源类型" clearable style="width: 160px" @change="loadResources">
          <el-option v-for="t in resourceTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button v-perm="'perm:create'" :icon="Plus" type="primary" @click="openAddResource">登记资源</el-button>
      </div>
      <el-table :data="resources" v-loading="loading" stripe>
        <el-table-column prop="resourceType" label="类型" width="120" />
        <el-table-column prop="resourceKey" label="资源Key" min-width="140" />
        <el-table-column prop="resourceName" label="名称" min-width="140" />
        <el-table-column prop="owner" label="属主" width="110" />
        <el-table-column prop="tenantId" label="租户" width="110" />
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button v-perm="'perm:delete'" size="small" link type="danger" @click="deleteResource(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 授权矩阵 -->
    <el-card v-if="tab === 'permissions'" shadow="never">
      <div class="toolbar">
        <el-button v-perm="'perm:create'" :icon="Plus" type="primary" @click="openGrant">新增授权</el-button>
      </div>
      <el-table :data="permissions" v-loading="loading" stripe>
        <el-table-column prop="roleId" label="角色ID" min-width="140" />
        <el-table-column prop="resourceType" label="资源类型" width="120" />
        <el-table-column prop="resourceKey" label="资源Key" width="110">
          <template #default="{ row }">{{ row.resourceKey || '*' }}</template>
        </el-table-column>
        <el-table-column prop="action" label="动作" width="110">
          <template #default="{ row }"><el-tag size="small">{{ row.action }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="scope" label="范围" width="100">
          <template #default="{ row }">
            <el-tag :type="row.scope === 'ALLOW' ? 'success' : 'danger'" size="small">{{ row.scope }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button v-perm="'perm:delete'" size="small" link type="danger" @click="revoke(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 登记资源 -->
    <el-dialog v-model="addVisible" title="登记资源" width="460px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="资源类型">
          <el-select v-model="form.resourceType" style="width: 100%">
            <el-option v-for="t in resourceTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源Key"><el-input v-model="form.resourceKey" /></el-form-item>
        <el-form-item label="资源名称"><el-input v-model="form.resourceName" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="doAddResource">登记</el-button>
      </template>
    </el-dialog>

    <!-- 新增授权 -->
    <el-dialog v-model="grantVisible" title="新增授权" width="460px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="角色ID"><el-input v-model="grant.roleId" /></el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="grant.resourceType" style="width: 100%">
            <el-option v-for="t in resourceTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源Key"><el-input v-model="grant.resourceKey" placeholder="* 代表全部" /></el-form-item>
        <el-form-item label="动作">
          <el-select v-model="grant.action" style="width: 100%">
            <el-option label="READ" value="READ" />
            <el-option label="WRITE" value="WRITE" />
            <el-option label="EXECUTE" value="EXECUTE" />
            <el-option label="ADMIN" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="grant.scope" style="width: 100%">
            <el-option label="ALLOW" value="ALLOW" />
            <el-option label="DENY" value="DENY" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" @click="doGrant">授权</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { permApi } from '@/api/modules/perm'

const tab = ref('resources')
const loading = ref(false)
const resources = ref([])
const permissions = ref([])
const typeFilter = ref('')
const resourceTypes = ['workflow', 'table', 'datasource', 'api', 'indicator', 'quality', 'tag', 'role']

const addVisible = ref(false)
const form = ref({ resourceType: 'workflow', resourceKey: '', resourceName: '' })
const grantVisible = ref(false)
const grant = ref({ roleId: '', resourceType: 'workflow', resourceKey: '*', action: 'READ', scope: 'ALLOW' })

async function loadResources() {
  loading.value = true
  try {
    resources.value = await permApi.resources(typeFilter.value || undefined) || []
  } catch (e) { ElMessage.error('加载资源失败') } finally { loading.value = false }
}

async function loadPermissions() {
  loading.value = true
  try {
    permissions.value = await permApi.permissions() || []
  } catch (e) { ElMessage.error('加载授权失败') } finally { loading.value = false }
}

async function deleteResource(row) {
  try {
    await permApi.deleteResource(row.id)
    ElMessage.success('已删除')
    await loadResources()
  } catch (e) { ElMessage.error(e.message || '删除失败') }
}

function openAddResource() {
  form.value = { resourceType: 'workflow', resourceKey: '', resourceName: '' }
  addVisible.value = true
}

async function doAddResource() {
  try {
    await permApi.registerResource(form.value)
    ElMessage.success('已登记')
    addVisible.value = false
    await loadResources()
  } catch (e) { ElMessage.error(e.message || '登记失败') }
}

function openGrant() {
  grant.value = { roleId: '', resourceType: 'workflow', resourceKey: '*', action: 'READ', scope: 'ALLOW' }
  grantVisible.value = true
}

async function doGrant() {
  if (!grant.value.roleId || !grant.value.resourceType) {
    ElMessage.warning('请填写角色与资源类型')
    return
  }
  try {
    await permApi.grant(grant.value)
    ElMessage.success('已授权')
    grantVisible.value = false
    await loadPermissions()
  } catch (e) { ElMessage.error(e.message || '授权失败') }
}

async function revoke(row) {
  try {
    await permApi.revoke(row.id)
    ElMessage.success('已撤销')
    await loadPermissions()
  } catch (e) { ElMessage.error(e.message || '撤销失败') }
}

onMounted(async () => {
  await loadResources()
  await loadPermissions()
})
</script>

<style scoped>
.perm-page { max-width: 1280px; }
.page-header { margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.page-desc { font-size: 13px; color: #9ca3af; margin: 4px 0 0; }
.toolbar { display: flex; gap: 8px; margin-bottom: 12px; }
</style>