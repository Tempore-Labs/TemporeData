<template>
  <div class="page">
    <PageHeader title="系统管理" subtitle="用户、角色、租户与组织架构管理">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate(tab)">+ 新建</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <el-skeleton v-if="userLoading" :rows="6" animated />
        <DataTable v-else :data="users">
          <el-table-column prop="username" label="用户名" min-width="140">
            <template #default="{ row }"><span class="mono">{{ row.username }}</span></template>
          </el-table-column>
          <el-table-column prop="nickname" label="昵称" width="120" />
          <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
          <el-table-column prop="phone" label="手机" min-width="140" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <StatusBadge :status="row.status === 0 ? 'closed' : 'success'" :text="row.status === 0 ? '停用' : '启用'" />
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="150">
            <template #default="{ row }"><span class="mono muted">{{ row.createdAt || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" text type="danger" :disabled="row.username === 'admin'" @click="delUser(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>

      <!-- 角色管理 -->
      <el-tab-pane label="角色管理" name="roles">
        <el-skeleton v-if="roleLoading" :rows="5" animated />
        <DataTable v-else :data="roles">
          <el-table-column prop="code" label="角色编码" min-width="160">
            <template #default="{ row }"><span class="mono">{{ row.code }}</span></template>
          </el-table-column>
          <el-table-column prop="name" label="名称" width="140" />
          <el-table-column prop="dataScope" label="数据范围" width="120">
            <template #default="{ row }">{{ row.dataScope || '—' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
          <el-table-column label="内置" width="90">
            <template #default="{ row }">
              <StatusBadge :status="row.protectedRole === true ? 'failed' : 'success'" :text="row.protectedRole === true ? '是' : '否'" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openRole(row)">编辑</el-button>
              <el-button size="small" text type="danger" :disabled="row.protectedRole === true" @click="delRole(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>

      <!-- 租户管理 -->
      <el-tab-pane label="租户管理" name="tenants">
        <el-skeleton v-if="tenantLoading" :rows="5" animated />
        <DataTable v-else :data="tenants">
          <el-table-column prop="name" label="租户名称" min-width="160" />
          <el-table-column prop="code" label="编码" width="140">
            <template #default="{ row }"><span class="mono">{{ row.code }}</span></template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="contactName" label="联系人" width="110" />
          <el-table-column prop="contactEmail" label="联系邮箱" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <StatusBadge :status="row.status === 0 ? 'closed' : 'success'" :text="row.status === 0 ? '停用' : '启用'" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openTenant(row)">编辑</el-button>
              <el-button size="small" text type="danger" :disabled="row.code === 't-demo'" @click="delTenant(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>

      <!-- 组织架构 -->
      <el-tab-pane label="组织架构" name="orgs">
        <el-skeleton v-if="orgLoading" :rows="5" animated />
        <DataTable v-else :data="orgs">
          <el-table-column prop="nickname" label="成员昵称" min-width="160" />
          <el-table-column prop="username" label="用户名" min-width="140">
            <template #default="{ row }"><span class="mono">{{ row.username || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="createTime" label="加入时间" min-width="150">
            <template #default="{ row }"><span class="mono muted">{{ row.createTime || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openOrg(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="delOrg(row)">删除</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>
    </el-tabs>

    <!-- create/edit dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <!-- User form -->
      <el-form v-if="tab === 'users'" :model="userForm" label-width="90px">
        <el-form-item label="用户名" required>
          <el-input v-model="userForm.username" :disabled="!!editing" />
        </el-form-item>
        <el-form-item v-if="!editing" label="密码">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="userForm.nickname" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="userForm.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- Role form -->
      <el-form v-else-if="tab === 'roles'" :model="roleForm" label-width="90px">
        <el-form-item label="编码" required>
          <el-input v-model="roleForm.code" :disabled="!!editing" placeholder="如 ROLE_DEVOPS"/>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="roleForm.name" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="roleForm.dataScope" style="width: 100%">
            <el-option v-for="d in ['ALL', 'SELF', 'TENANT', 'ORG']" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="roleForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <!-- Tenant form -->
      <el-form v-else-if="tab === 'tenants'" :model="tenantForm" label-width="100px">
        <el-form-item label="租户名称" required>
          <el-input v-model="tenantForm.name" />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="tenantForm.code" :disabled="!!editing" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="tenantForm.description" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="tenantForm.contactName" />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="tenantForm.contactEmail" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="tenantForm.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- Org form -->
      <el-form v-else :model="orgForm" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="orgForm.username" />
        </el-form-item>
        <el-form-item label="昵称" required>
          <el-input v-model="orgForm.nickname" />
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
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQueryClient } from '@tanstack/vue-query'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { sysApi, type OrgEntity, type RoleEntity, type TenantEntity, type UserRes } from '@/api/system'

const queryClient = useQueryClient()
const tab = ref('users')

// lists
const users = ref<UserRes[]>([])
const userLoading = ref(false)
const roles = ref<RoleEntity[]>([])
const roleLoading = ref(false)
const tenants = ref<TenantEntity[]>([])
const tenantLoading = ref(false)
const orgs = ref<OrgEntity[]>([])
const orgLoading = ref(false)

// dialog
const dialogVisible = ref(false)
const editing = ref<Record<string, unknown> | null>(null)
const saving = ref(false)
const userForm = ref<Partial<UserRes> & { password?: string }>({})
const roleForm = ref<Partial<RoleEntity>>({})
const tenantForm = ref<Partial<TenantEntity>>({})
const orgForm = ref<Partial<OrgEntity>>({})

const dialogTitle = computed(() => {
  const labels: Record<string, string> = { users: '用户', roles: '角色', tenants: '租户', orgs: '组织成员' }
  return `${editing.value ? '编辑' : '新建'}${labels[tab.value] || ''}`
})

async function loadUsers() {
  userLoading.value = true
  try {
    users.value = await sysApi.users()
  } finally {
    userLoading.value = false
  }
}
async function loadRoles() {
  roleLoading.value = true
  try {
    roles.value = await sysApi.roles()
  } finally {
    roleLoading.value = false
  }
}
async function loadTenants() {
  tenantLoading.value = true
  try {
    tenants.value = await sysApi.tenants()
  } finally {
    tenantLoading.value = false
  }
}
async function loadOrgs() {
  orgLoading.value = true
  try {
    orgs.value = await sysApi.orgs()
  } finally {
    orgLoading.value = false
  }
}
loadUsers()

function onTabChange(name: string | number) {
  if (name === 'roles') loadRoles()
  else if (name === 'tenants') loadTenants()
  else if (name === 'orgs') loadOrgs()
}

function openCreate(type: string) {
  editing.value = null
  if (type === 'users') {
    userForm.value = { username: '', password: '', nickname: '', email: '', phone: '', status: 1 }
  } else if (type === 'roles') {
    roleForm.value = { code: '', name: '', dataScope: 'ALL', remark: '' }
  } else if (type === 'tenants') {
    tenantForm.value = { name: '', code: '', description: '', contactName: '', contactEmail: '', status: 1 }
  } else {
    orgForm.value = { username: '', nickname: '' }
  }
  dialogVisible.value = true
}

function openEdit(row: UserRes) {
  editing.value = row as unknown as Record<string, unknown>
  userForm.value = { ...row }
  dialogVisible.value = true
}
function openRole(row: RoleEntity) {
  editing.value = row as unknown as Record<string, unknown>
  roleForm.value = { ...row }
  dialogVisible.value = true
}
function openTenant(row: TenantEntity) {
  editing.value = row as unknown as Record<string, unknown>
  tenantForm.value = { ...row }
  dialogVisible.value = true
}
function openOrg(row: OrgEntity) {
  editing.value = row as unknown as Record<string, unknown>
  orgForm.value = { ...row }
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (tab.value === 'users') {
      if (!userForm.value.username) {
        ElMessage.warning('用户名必填')
        return
      }
      if (editing.value) await sysApi.updateUser(editing.value.id as string, userForm.value)
      else await sysApi.createUser(userForm.value)
      await loadUsers()
    } else if (tab.value === 'roles') {
      if (!roleForm.value.code || !roleForm.value.name) {
        ElMessage.warning('编码与名称必填')
        return
      }
      if (editing.value) await sysApi.updateRole(editing.value.id as string, roleForm.value)
      else await sysApi.createRole(roleForm.value)
      await loadRoles()
    } else if (tab.value === 'tenants') {
      if (!tenantForm.value.name || !tenantForm.value.code) {
        ElMessage.warning('名称与编码必填')
        return
      }
      if (editing.value) await sysApi.updateTenant(editing.value.id as string, tenantForm.value)
      else await sysApi.createTenant(tenantForm.value)
      await loadTenants()
    } else {
      if (!orgForm.value.nickname) {
        ElMessage.warning('昵称必填')
        return
      }
      if (editing.value) await sysApi.updateOrg(orgForm.value)
      else await sysApi.createOrg(orgForm.value)
      await loadOrgs()
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['system'] })
  } finally {
    saving.value = false
  }
}

async function delUser(row: UserRes) {
  await ElMessageBox.confirm(`确认删除用户 ${row.username} ？`, '提示', { type: 'warning' })
  await sysApi.deleteUser(row.id)
  ElMessage.success('已删除')
  await loadUsers()
}
async function delRole(row: RoleEntity) {
  await ElMessageBox.confirm(`确认删除角色 ${row.name} ？`, '提示', { type: 'warning' })
  await sysApi.deleteRole(row.id)
  ElMessage.success('已删除')
  await loadRoles()
}
async function delTenant(row: TenantEntity) {
  await ElMessageBox.confirm(`确认删除租户 ${row.name} ？`, '提示', { type: 'warning' })
  await sysApi.deleteTenant(row.id)
  ElMessage.success('已删除')
  await loadTenants()
}
async function delOrg(row: OrgEntity) {
  await ElMessageBox.confirm(`确认移除成员 ${row.nickname} ？`, '提示', { type: 'warning' })
  await sysApi.deleteOrg(row.id)
  ElMessage.success('已移除')
  await loadOrgs()
}
</script>

<style scoped>
.page {
  padding: 24px;
}
.mono {
  font-family: var(--td-font-mono);
}
.muted {
  color: var(--td-text-4);
}
</style>