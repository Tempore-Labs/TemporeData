<template>
  <div class="tenant-page">
    <PageHeader title="租户成员" desc="管理多租户环境，支持租户创建、成员管理和资源隔离">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openAddDialog">新增租户</el-button>
      </template>
    </PageHeader>

    <div class="page-body">
      <el-table :data="pagedData" v-loading="loading" stripe>
        <el-table-column prop="name" label="租户名称" min-width="150" />
        <el-table-column prop="code" label="租户编码" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.code" effect="dark">{{ row.code }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactEmail" label="联系邮箱" min-width="170" show-overflow-tooltip />
        <el-table-column prop="memberCount" label="成员数" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <StatusTag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" :label="row.status === 'ACTIVE' ? '启用' : '停用'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="success" link :icon="User" @click="openMembersDialog(row)">成员</el-button>
            <el-popconfirm title="确定删除该租户吗？删除将同时移除其下所有成员。" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="tableData.length"
          layout="total, sizes, prev, pager, next"
          @size-change="currentPage = 1"
        />
      </div>
    </div>

    <!-- 新增/编辑租户 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑租户' : '新增租户'" width="500px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="租户名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入租户名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="租户编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入租户编码（创建后不可修改）" maxlength="32" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人" maxlength="32" />
        </el-form-item>
        <el-form-item label="联系邮箱" prop="contactEmail">
          <el-input v-model="form.contactEmail" placeholder="请输入联系邮箱" maxlength="64" />
        </el-form-item>
        <el-form-item label="备注" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入租户描述" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 成员管理 -->
    <el-dialog v-model="memberVisible" :title="`成员管理 · ${currentTenant?.name || ''}`" width="640px" :close-on-click-modal="false">
      <!-- 添加成员 -->
      <el-card shadow="never" class="member-add-card">
        <div class="member-add-row">
          <el-input v-model="memberForm.username" placeholder="用户名" style="width: 150px" maxlength="64" />
          <el-input v-model="memberForm.password" type="password" show-password placeholder="初始密码" style="width: 150px" maxlength="64" />
          <el-select v-model="memberForm.roleCode" placeholder="角色" style="width: 120px" clearable>
            <el-option v-for="r in roleOptions" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
          <el-tree-select
            v-model="memberForm.orgId"
            :data="orgTreeOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            default-expand-all
            clearable
            placeholder="所属部门"
            style="width: 150px"
          />
          <el-button type="primary" :loading="addLoading" @click="handleAddMember">添加成员</el-button>
        </div>
        <div class="member-add-hint">创建的账号可直接登录平台；不选角色时默认分配 DEVELOPER；用户名全局唯一</div>
      </el-card>

      <el-table :data="members" v-loading="membersLoading" stripe>
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column label="所属部门" min-width="120">
          <template #default="{ row }">
            <el-tag v-if="row.orgName" size="small" type="info" effect="light">{{ row.orgName }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" size="small" style="margin-right: 6px">{{ r }}</el-tag>
            <span v-if="!row.roles || row.roles.length === 0">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="statusLoadingId === row.id"
              @change="(val) => handleToggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="加入时间" width="170" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-popconfirm title="确定移除该成员吗？" @confirm="handleRemoveMember(row.id)">
              <template #reference>
                <el-button type="danger" link :icon="Delete">移除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, User } from '@element-plus/icons-vue'
import { tenantApi } from '@/api/modules/tenant'
import { roleApi } from '@/api/modules/role'
import { orgApi } from '@/api/modules/org'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return tableData.value.slice(start, start + pageSize.value)
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({ name: '', code: '', contactName: '', contactEmail: '', description: '' })
const rules = {
  name: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  contactEmail: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const memberVisible = ref(false)
const membersLoading = ref(false)
const members = ref([])
const addLoading = ref(false)
const statusLoadingId = ref('')
const currentTenant = ref(null)
const memberForm = reactive({ username: '', password: '', roleCode: '', orgId: '' })
const roleOptions = ref([])
const orgTreeOptions = ref([])

async function fetchData() {
  loading.value = true
  try {
    tableData.value = await tenantApi.list()
  } catch (err) {
    ElMessage.error(err.message || '加载租户列表失败')
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const res = await roleApi.list()
    roleOptions.value = Array.isArray(res) ? res : res?.data || []
  } catch (_) { /* ignore */ }
}

function openAddDialog() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, { name: '', code: '', contactName: '', contactEmail: '', description: '' })
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    name: row.name,
    code: row.code || '',
    contactName: row.contactName || '',
    contactEmail: row.contactEmail || '',
    description: row.description || ''
  })
  dialogVisible.value = true
}

async function openMembersDialog(row) {
  currentTenant.value = row
  memberVisible.value = true
  Object.assign(memberForm, { username: '', password: '', roleCode: '', orgId: '' })
  await Promise.all([fetchMembers(row.id), loadRoles(), loadOrgTree(row.id)])
}

async function loadOrgTree(tenantId) {
  try {
    orgTreeOptions.value = await orgApi.tree(tenantId) || []
  } catch (_) {
    orgTreeOptions.value = []
  }
}

async function fetchMembers(tenantId) {
  membersLoading.value = true
  try {
    members.value = await tenantApi.listMembers(tenantId)
  } catch (err) {
    ElMessage.error(err.message || '加载成员失败')
  } finally {
    membersLoading.value = false
  }
}

async function handleAddMember() {
  if (!memberForm.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!memberForm.password) {
    ElMessage.warning('请输入初始密码')
    return
  }
  addLoading.value = true
  try {
    await tenantApi.addMember(currentTenant.value.id, { ...memberForm })
    ElMessage.success('成员添加成功')
    Object.assign(memberForm, { username: '', password: '', roleCode: '', orgId: '' })
    await fetchMembers(currentTenant.value.id)
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '添加成员失败')
  } finally {
    addLoading.value = false
  }
}

async function handleRemoveMember(userId) {
  try {
    await tenantApi.removeMember(currentTenant.value.id, userId)
    ElMessage.success('成员已移除')
    await fetchMembers(currentTenant.value.id)
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '移除失败')
  }
}

async function handleToggleStatus(row, enable) {
  if (enable === false) {
    try {
      const user = JSON.parse(localStorage.getItem('td_user') || '{}')
      if (user.username === row.username) {
        ElMessage.warning('不能停用当前登录账号')
        return
      }
    } catch { /* ignore */ }
  }
  statusLoadingId.value = row.id
  try {
    await tenantApi.setMemberStatus(currentTenant.value.id, row.id, enable ? 1 : 0)
    ElMessage.success(enable ? '账号已启用' : '账号已停用')
    await fetchMembers(currentTenant.value.id)
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  } finally {
    statusLoadingId.value = ''
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await tenantApi.update(editId.value, { ...form })
      ElMessage.success('租户更新成功')
    } else {
      await tenantApi.create({ ...form })
      ElMessage.success('租户创建成功')
    }
    dialogVisible.value = false
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(id) {
  try {
    await tenantApi.delete(id)
    ElMessage.success('租户已删除')
    await fetchData()
  } catch (err) {
    ElMessage.error(err.message || '删除失败')
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.tenant-page { max-width: 1200px; }
.page-body {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.member-add-card {
  margin-bottom: 16px;
  background: #f8fafc;
  border-radius: 8px;
}
.member-add-card :deep(.el-card__body) {
  padding: 14px 16px;
}
.member-add-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.member-add-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 8px;
}
</style>