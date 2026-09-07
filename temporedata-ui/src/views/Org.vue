<template>
  <div class="org-page">
    <div class="page-header">
      <h2><el-icon class="header-icon"><Share /></el-icon>组织架构</h2>
      <p>部门树与成员管理</p>
    </div>

    <div class="org-layout">
      <!-- 左：部门树 -->
      <div class="org-tree-card">
        <div class="tree-toolbar">
          <span class="tree-title">部门树</span>
          <el-button type="primary" size="small" :icon="Plus" @click="openCreateDialog()">新增部门</el-button>
        </div>
        <el-tree
          v-loading="treeLoading"
          :data="orgTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          highlight-current
          default-expand-all
          :expand-on-click-node="false"
          @node-click="handleNodeClick"
        >
          <template #default="{ data }">
            <div class="tree-node">
              <span class="tree-node-name">{{ data.name }}</span>
              <el-tag v-if="data.memberCount" size="small" type="info" effect="plain" class="tree-node-count">{{ data.memberCount }}</el-tag>
            </div>
          </template>
        </el-tree>
      </div>

      <!-- 右：部门详情 + 成员 -->
      <div class="org-detail-card" v-loading="detailLoading">
        <template v-if="currentOrg">
          <div class="detail-header">
            <div class="detail-title">
              <h3>{{ currentOrg.name }}</h3>
              <el-tag v-if="responsibleUser" type="success" effect="light" size="small" class="responsible-tag">
                <el-icon><User /></el-icon>&nbsp;负责人：{{ responsibleUser }}
              </el-tag>
              <el-tag v-else type="info" effect="plain" size="small">未设置负责人</el-tag>
              <el-tag v-if="defaultRoleName" type="warning" effect="light" size="small">
                默认角色：{{ defaultRoleName }}
              </el-tag>
            </div>
            <div class="detail-actions">
              <el-button size="small" :icon="Plus" @click="openCreateDialog(currentOrg.id)">子部门</el-button>
              <el-button size="small" :icon="Edit" @click="openEditDialog">编辑</el-button>
              <el-button size="small" type="danger" plain :icon="Delete" @click="handleDelete">删除</el-button>
            </div>
          </div>

          <!-- 成员列表 -->
          <div class="member-section">
            <div class="member-toolbar">
              <span class="member-title">部门成员（{{ members.length }}）</span>
              <div class="member-add-row">
                <el-select
                  v-model="addUserId"
                  placeholder="选择用户加入部门"
                  filterable
                  clearable
                  style="width: 220px"
                >
                  <el-option v-for="u in candidateUsers" :key="u.id" :label="u.username" :value="u.id" />
                </el-select>
                <el-button type="primary" size="small" :disabled="!addUserId" @click="handleAddMember">添加</el-button>
              </div>
            </div>

            <el-table :data="members" stripe size="small" style="width: 100%">
              <el-table-column prop="username" label="用户名" min-width="120" />
              <el-table-column label="角色" min-width="150">
                <template #default="{ row }">
                  <el-tag v-for="r in row.roles" :key="r" size="small" effect="light" style="margin-right: 4px">{{ r }}</el-tag>
                  <span v-if="!row.roles || row.roles.length === 0">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="加入时间" min-width="150" />
              <el-table-column label="操作" width="200">
                <template #default="{ row }">
                  <el-button size="small" link type="success" :icon="User" @click="handleSetResponsible(row)">设为负责人</el-button>
                  <el-popconfirm title="确定移出该部门？" @confirm="handleRemoveMember(row.id)">
                    <template #reference>
                      <el-button size="small" link type="danger" :icon="Delete">移除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>

        <div v-else class="detail-empty">
          <el-empty description="请选择左侧部门查看成员" />
        </div>
      </div>
    </div>

    <!-- 新增/编辑部门 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" width="480px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入部门名称" maxlength="64" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="上级部门" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="orgTreeOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            default-expand-all
            clearable
            placeholder="不选则为顶级部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="负责人" prop="responsibleUserId">
          <el-select v-model="form.responsibleUserId" clearable filterable placeholder="从现有用户中选择" style="width: 100%">
            <el-option v-for="u in candidateUsers" :key="u.id" :label="u.username" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认角色" prop="defaultRoleId">
          <el-select v-model="form.defaultRoleId" clearable placeholder="新成员加入该部门自动获得" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="`${r.name}（${r.code}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, User, Share } from '@element-plus/icons-vue'
import { orgApi } from '@/api/modules/org'
import { tenantApi } from '@/api/modules/tenant'
import { roleApi } from '@/api/modules/role'
import { ElMessage } from 'element-plus'

function currentTenantId() {
  try {
    const u = JSON.parse(localStorage.getItem('td_user') || '{}')
    return u.tenantId || 'DEFAULT'
  } catch {
    return 'DEFAULT'
  }
}

const treeLoading = ref(false)
const detailLoading = ref(false)
const orgTree = ref([])
const currentOrg = ref(null)
const members = ref([])
const responsibleUser = ref('')
const candidateUsers = ref([])
const roleOptions = ref([])
const addUserId = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({ name: '', parentId: '', responsibleUserId: '', defaultRoleId: '', remark: '' })
const rules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

const defaultRoleName = computed(() => {
  if (!currentOrg.value?.defaultRoleId) return ''
  const r = roleOptions.value.find(x => x.id === currentOrg.value.defaultRoleId)
  return r ? r.name : ''
})

const orgTreeOptions = computed(() => orgTree.value)

async function fetchTree() {
  treeLoading.value = true
  try {
    orgTree.value = await orgApi.tree(currentTenantId())
  } catch (e) {
    ElMessage.error(e.message || '加载组织架构失败')
  } finally {
    treeLoading.value = false
  }
}

async function loadRoles() {
  try {
    const res = await roleApi.list()
    roleOptions.value = Array.isArray(res) ? res : res?.data || []
  } catch { /* ignore */ }
}

async function loadCandidateUsers() {
  try {
    const tenants = await tenantApi.list()
    const users = []
    for (const t of tenants) {
      const ms = await tenantApi.listMembers(t.id)
      ms.forEach(m => users.push({ id: m.id, username: m.username, tenantId: t.id }))
    }
    candidateUsers.value = users
  } catch { /* ignore */ }
}

async function handleNodeClick(data) {
  currentOrg.value = data
  responsibleUser.value = ''
  await loadMembers(data.id)
}

async function loadMembers(orgId) {
  detailLoading.value = true
  try {
    members.value = await orgApi.members(orgId) || []
    const resp = await orgApi.tree(currentTenantId())
    const found = findOrg(resp, orgId)
    if (found?.responsibleUserId) {
      const ru = members.value.find(m => m.id === found.responsibleUserId)
      responsibleUser.value = ru ? ru.username : '已离职用户'
    }
  } catch (e) {
    ElMessage.error(e.message || '加载成员失败')
  } finally {
    detailLoading.value = false
  }
}

function findOrg(nodes, id) {
  for (const n of nodes) {
    if (n.id === id) return n
    if (n.children) {
      const r = findOrg(n.children, id)
      if (r) return r
    }
  }
  return null
}

function openCreateDialog(parentId) {
  isEdit.value = false
  editId.value = null
  Object.assign(form, { name: '', parentId: parentId || '', responsibleUserId: '', defaultRoleId: '', remark: '' })
  dialogVisible.value = true
}

function openEditDialog() {
  isEdit.value = true
  editId.value = currentOrg.value.id
  Object.assign(form, {
    name: currentOrg.value.name,
    parentId: currentOrg.value.parentId || '',
    responsibleUserId: currentOrg.value.responsibleUserId || '',
    defaultRoleId: currentOrg.value.defaultRoleId || '',
    remark: currentOrg.value.remark || ''
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await orgApi.update(editId.value, { ...form })
      ElMessage.success('部门更新成功')
    } else {
      await orgApi.create({ ...form, tenantId: currentTenantId() })
      ElMessage.success('部门创建成功')
    }
    dialogVisible.value = false
    await fetchTree()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete() {
  try {
    await orgApi.delete(currentOrg.value.id)
    ElMessage.success('部门已删除')
    currentOrg.value = null
    members.value = []
    await fetchTree()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

async function handleAddMember() {
  if (!addUserId.value) return
  try {
    await orgApi.assignMember(currentOrg.value.id, addUserId.value)
    ElMessage.success('成员已加入部门')
    addUserId.value = ''
    await loadMembers(currentOrg.value.id)
  } catch (e) {
    ElMessage.error(e.message || '添加失败')
  }
}

async function handleRemoveMember(userId) {
  try {
    await orgApi.removeMember(currentOrg.value.id, userId)
    ElMessage.success('成员已移出')
    await loadMembers(currentOrg.value.id)
  } catch (e) {
    ElMessage.error(e.message || '移除失败')
  }
}

async function handleSetResponsible(row) {
  try {
    await orgApi.setResponsible(currentOrg.value.id, row.id)
    responsibleUser.value = row.username
    ElMessage.success('已设为部门负责人')
    await fetchTree()
  } catch (e) {
    ElMessage.error(e.message || '设置失败')
  }
}

onMounted(async () => {
  await Promise.all([fetchTree(), loadCandidateUsers(), loadRoles()])
})
</script>

<style scoped>
.org-page { max-width: 1400px; }
.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px; }
.page-header h2 { font-size: 20px; margin: 0; display: flex; align-items: center; gap: 6px; }
.page-header p { margin: 0; color: #9ca3af; font-size: 13px; }
.header-icon { color: #6366f1; }

.org-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  align-items: start;
}

.org-tree-card, .org-detail-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  padding: 16px;
  min-height: 480px;
}

.tree-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.tree-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  padding-right: 8px;
}

.tree-node-name {
  font-size: 14px;
  color: #374151;
}

.tree-node-count {
  border-radius: 10px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-title h3 {
  margin: 0;
  font-size: 16px;
  color: #111827;
}

.responsible-tag {
  display: inline-flex;
  align-items: center;
}

.detail-actions {
  display: flex;
  gap: 8px;
}

.member-section .member-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 10px;
}

.member-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.member-add-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.detail-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}
</style>
