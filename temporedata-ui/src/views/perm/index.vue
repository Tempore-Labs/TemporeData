<template>
  <div class="page">
    <PageHeader title="权限中心" subtitle="资源注册、角色授权与访问校验">
      <template #actions>
        <el-button size="small" type="primary" @click="regVisible = true">+ 注册资源</el-button>
        <el-button size="small" @click="grantVisible = true">+ 授予权限</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- 资源管理 -->
      <el-tab-pane label="资源管理" name="resources">
        <div class="filter-bar">
          <el-input v-model="typeFilter" placeholder="资源类型筛选" size="small" style="width: 180px" clearable @keyup.enter="loadResources" />
          <el-button size="small" @click="loadResources">筛选</el-button>
        </div>
        <el-skeleton v-if="resLoading" :rows="6" animated />
        <DataTable v-else :data="resources">
          <el-table-column prop="resourceType" label="类型" width="130">
            <template #default="{ row }"><el-tag size="small">{{ row.resourceType }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="resourceKey" label="资源键" min-width="160">
            <template #default="{ row }"><span class="mono">{{ row.resourceKey }}</span></template>
          </el-table-column>
          <el-table-column prop="resourceName" label="资源名" min-width="150" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="owner" label="Owner" width="110" />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="danger" @click="delResource(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>

      <!-- 授权记录 -->
      <el-tab-pane label="授权记录" name="perms">
        <div class="filter-bar">
          <el-input v-model="roleFilter" placeholder="角色 ID，如 role-admin" size="small" style="width: 200px" clearable @change="loadPerms" />
          <el-button size="small" @click="loadPerms">查询</el-button>
        </div>
        <el-skeleton v-if="permLoading" :rows="6" animated />
        <DataTable v-else :data="perms">
          <el-table-column prop="roleId" label="角色" min-width="130">
            <template #default="{ row }"><span class="mono">{{ row.roleId }}</span></template>
          </el-table-column>
          <el-table-column prop="resourceType" label="资源类型" width="130">
            <template #default="{ row }"><el-tag size="small">{{ row.resourceType }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="resourceKey" label="资源键" min-width="150">
            <template #default="{ row }"><span class="mono">{{ row.resourceKey }}</span></template>
          </el-table-column>
          <el-table-column prop="action" label="动作" width="110">
            <template #default="{ row }"><el-tag size="small" :type="actionTag(row.action)">{{ row.action }}</el-tag></template>
          </el-table-column>
          <el-table-column label="范围" width="110">
            <template #default="{ row }">
              <StatusBadge :status="row.scope === 'DENY' ? 'failed' : 'success'" :text="row.scope" />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="授予时间" min-width="150">
            <template #default="{ row }"><span class="mono muted">{{ row.createTime || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="danger" @click="revoke(row)">撤销</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>

      <!-- 校验沙盒 -->
      <el-tab-pane label="校验沙盒" name="verify">
        <Card class="verify-card">
          <div class="panel-title">访问校验</div>
          <el-form :model="vf" label-width="110px" size="small" label-position="top">
            <div class="vf-grid">
              <el-form-item label="角色 IDs">
                <el-input v-model="vf.roleIds" placeholder="逗号分隔，如 role-admin,role-member" />
              </el-form-item>
              <el-form-item label="超管">
                <el-switch v-model="vf.superAdmin" />
              </el-form-item>
              <el-form-item label="资源类型">
                <el-input v-model="vf.resourceType" placeholder="如 DATASET" />
              </el-form-item>
              <el-form-item label="动作">
                <el-input v-model="vf.action" placeholder="如 READ" />
              </el-form-item>
              <el-form-item label="资源键">
                <el-input v-model="vf.resourceKey" placeholder="如 * 或具体 key" />
              </el-form-item>
            </div>
            <el-button type="primary" :loading="verifying" @click="doVerify">校验权限</el-button>
          </el-form>
          <div v-if="verifyResult !== null" class="verify-result" :class="verifyResult ? 'ok' : 'deny'">
            <div class="res-title">{{ verifyResult ? '允许 (ALLOW)' : '拒绝 (DENY)' }}</div>
            <div class="res-sub">基于角色/超管标志 + 资源类型/动作/键 判定</div>
          </div>
        </Card>
      </el-tab-pane>
    </el-tabs>

    <!-- register resource dialog -->
    <el-dialog v-model="regVisible" title="注册资源" width="480px">
      <el-form :model="regForm" label-width="90px">
        <el-form-item label="资源类型" required>
          <el-input v-model="regForm.resourceType" placeholder="如 DATASET" />
        </el-form-item>
        <el-form-item label="资源键" required>
          <el-input v-model="regForm.resourceKey" placeholder="如 ds:orders" />
        </el-form-item>
        <el-form-item label="资源名">
          <el-input v-model="regForm.resourceName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="regForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="regVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doRegister">注册</el-button>
      </template>
    </el-dialog>

    <!-- grant permission dialog -->
    <el-dialog v-model="grantVisible" title="授予权限" width="480px">
      <el-form :model="grantForm" label-width="90px">
        <el-form-item label="角色 ID" required>
          <el-input v-model="grantForm.roleId" />
        </el-form-item>
        <el-form-item label="资源类型" required>
          <el-input v-model="grantForm.resourceType" />
        </el-form-item>
        <el-form-item label="资源键">
          <el-input v-model="grantForm.resourceKey" placeholder="* 表示全部" />
        </el-form-item>
        <el-form-item label="动作" required>
          <el-select v-model="grantForm.action" style="width: 100%">
            <el-option v-for="a in ['READ', 'WRITE', 'EXECUTE', 'ADMIN']" :key="a" :label="a" :value="a" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围">
          <el-radio-group v-model="grantForm.scope">
            <el-radio value="ALLOW">允许</el-radio>
            <el-radio value="DENY">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doGrant">授予</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import Card from '@/components/base/Card.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { permApi, type PermissionEntity, type ResourceEntity } from '@/api/perm'

const queryClient = useQueryClient()
const tab = ref('resources')

// resources
const resources = ref<ResourceEntity[]>([])
const resLoading = ref(false)
const typeFilter = ref('')

// perms
const perms = ref<PermissionEntity[]>([])
const permLoading = ref(false)
const roleFilter = ref('')

// verify
const verifying = ref(false)
const verifyResult = ref<boolean | null>(null)
const vf = reactive({ roleIds: 'role-admin', superAdmin: true, resourceType: 'DATASET', action: 'READ', resourceKey: '*' })

const regVisible = ref(false)
const grantVisible = ref(false)
const saving = ref(false)
const regForm = ref<Partial<ResourceEntity>>({})
const grantForm = ref<Partial<PermissionEntity>>({})

async function loadResources() {
  resLoading.value = true
  try {
    resources.value = await permApi.resources(typeFilter.value || undefined)
  } finally {
    resLoading.value = false
  }
}
async function loadPerms() {
  permLoading.value = true
  try {
    perms.value = await permApi.permissions(roleFilter.value || undefined)
  } finally {
    permLoading.value = false
  }
}
loadResources()

function onTabChange(name: string | number) {
  if (name === 'perms') loadPerms()
}

async function doRegister() {
  if (!regForm.value.resourceType || !regForm.value.resourceKey) {
    ElMessage.warning('类型与键必填')
    return
  }
  saving.value = true
  try {
    await permApi.registerResource(regForm.value as { resourceType: string; resourceKey: string })
    ElMessage.success('资源已注册')
    regVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['perm'] })
    await loadResources()
  } finally {
    saving.value = false
  }
}
async function doGrant() {
  if (!grantForm.value.roleId || !grantForm.value.resourceType || !grantForm.value.action) {
    ElMessage.warning('角色/类型/动作必填')
    return
  }
  saving.value = true
  try {
    await permApi.grant(grantForm.value)
    ElMessage.success('权限已授予')
    grantVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['perm'] })
    await loadPerms()
  } finally {
    saving.value = false
  }
}
async function delResource(row: ResourceEntity) {
  await ElMessageBox.confirm(`确认删除资源 ${row.resourceKey} ？`, '提示', { type: 'warning' })
  await permApi.deleteResource(row.id)
  ElMessage.success('已删除')
  await loadResources()
}
async function revoke(row: PermissionEntity) {
  await ElMessageBox.confirm(`确认撤销对 ${row.resourceKey} 的 ${row.action} 授权？`, '提示', { type: 'warning' })
  await permApi.revoke(row.id)
  ElMessage.success('已撤销')
  await loadPerms()
}
async function doVerify() {
  verifying.value = true
  try {
    verifyResult.value = await permApi.verify({
      roleIds: vf.roleIds,
      superAdmin: vf.superAdmin,
      resourceType: vf.resourceType,
      action: vf.action,
      resourceKey: vf.resourceKey,
    })
  } finally {
    verifying.value = false
  }
}
function actionTag(a?: string) {
  if (a === 'ADMIN') return 'danger'
  if (a === 'WRITE') return 'warning'
  if (a === 'EXECUTE') return 'info'
  return 'success'
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.verify-card {
  padding: 20px;
  max-width: 720px;
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--td-text-1);
  margin-bottom: 16px;
}
.vf-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 12px;
}
.verify-result {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid;
}
.verify-result.ok {
  background: #f0fdf4;
  border-color: #bbf7d0;
  color: #065f46;
}
.verify-result.deny {
  background: #fef2f2;
  border-color: #fecaca;
  color: #991b1b;
}
.res-title {
  font-weight: 600;
}
.res-sub {
  font-size: 12px;
  margin-top: 2px;
  opacity: 0.8;
}
.mono {
  font-family: var(--td-font-mono);
}
.muted {
  color: var(--td-text-4);
}
</style>