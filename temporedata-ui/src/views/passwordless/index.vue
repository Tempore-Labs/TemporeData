<template>
  <div class="page">
    <PageHeader title="免密登录" subtitle="免密登录方式配置与验证">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 新增配置</el-button>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />
    <DataTable v-else :data="rows">
      <el-table-column prop="loginType" label="登录方式" width="160">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.loginType || '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="config" label="配置" min-width="260" show-overflow-tooltip>
        <template #default="{ row }"><span class="mono">{{ row.config || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="info" @click="openDemo">验证</el-button>
        </template>
      </el-table-column>
    </DataTable>

    <el-divider content-position="left">验证演示</el-divider>
    <div class="demo-row">
      <el-input v-model="demoTarget" placeholder="手机/邮箱" size="small" style="max-width: 200px" />
      <el-button size="small" @click="sendCode">发送验证码</el-button>
      <el-input v-model="demoCode" placeholder="验证码" size="small" style="max-width: 160px" />
      <el-button size="small" type="primary" @click="verifyCode">校验</el-button>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑配置' : '新增配置'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="登录方式" required>
          <el-select v-model="form.loginType" style="width: 100%">
            <el-option v-for="s in ['SMS', 'EMAIL', 'OAUTH', 'MAGIC_LINK']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="配置">
          <el-input v-model="form.config" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in ['ENABLED', 'DISABLED']" :key="s" :label="s" :value="s" />
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
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import DataTable from '@/components/base/DataTable.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import { passwordlessApi, type PasswordlessConfig } from '@/api/ops2.module'

const rows = ref<PasswordlessConfig[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<PasswordlessConfig | null>(null)
const saving = ref(false)
const form = ref<Partial<PasswordlessConfig>>({})
const demoTarget = ref('')
const demoCode = ref('')

async function load() {
  loading.value = true
  try {
    rows.value = (await passwordlessApi.config()) || []
  } finally {
    loading.value = false
  }
}
onMounted(load)

function openCreate() {
  editing.value = null
  form.value = { loginType: 'SMS', config: '', status: 'ENABLED' }
  dialogVisible.value = true
}
function openEdit(row: PasswordlessConfig) {
  editing.value = row
  form.value = { ...row }
  dialogVisible.value = true
}
async function save() {
  if (!form.value.loginType) {
    ElMessage.warning('登录方式必填')
    return
  }
  saving.value = true
  try {
    await passwordlessApi.saveConfig(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function sendCode() {
  if (!demoTarget.value) {
    ElMessage.warning('请输入手机/邮箱')
    return
  }
  const res = await passwordlessApi.send({ target: demoTarget.value })
  ElMessage.info(`已发送，返回: ${res}`)
}
async function verifyCode() {
  if (!demoCode.value) {
    ElMessage.warning('请输入验证码')
    return
  }
  const ok = await passwordlessApi.verify({ target: demoTarget.value, code: demoCode.value })
  ElMessage.success(ok ? '校验通过' : '校验失败')
}
function openDemo() {
  /* demo inputs are already visible on the page */
}
</script>

<style scoped>
.page { padding: 24px; }
.demo-row { display: flex; gap: 8px; align-items: center; }
.mono { font-family: var(--td-font-mono); }
</style>