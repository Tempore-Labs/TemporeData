<template>
  <div class="page">
    <PageHeader title="Git 运维" subtitle="代码仓库绑定、触发构建与部署">
      <template #actions>
        <el-button size="small" type="primary" @click="openCreate">+ 绑定仓库</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" class="git-tabs">
      <el-tab-pane label="仓库" name="repos">
        <el-skeleton v-if="reposLoading" :rows="5" animated />
        <DataTable v-else :data="repos">
          <el-table-column prop="repoRef" label="仓库地址" min-width="220">
            <template #default="{ row }"><span class="mono">{{ row.repoRef }}</span></template>
          </el-table-column>
          <el-table-column prop="provider" label="平台" width="110">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ row.provider }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="branch" label="分支" width="120">
            <template #default="{ row }"><span class="mono">{{ row.branch || 'main' }}</span></template>
          </el-table-column>
          <el-table-column prop="environment" label="环境" width="100">
            <template #default="{ row }">{{ row.environment || '—' }}</template>
          </el-table-column>
          <el-table-column prop="autoTrigger" label="自动触发" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.autoTrigger ? 'success' : 'info'" effect="plain">
                {{ row.autoTrigger ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="enabled" label="启用" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="trigger(row)">构建</el-button>
              <el-button size="small" text type="warning" @click="syncRepo(row)">同步</el-button>
              <el-button size="small" text type="danger" @click="unbind(row)">解绑</el-button>
            </template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>

      <el-tab-pane label="构建记录" name="builds">
        <el-skeleton v-if="buildsLoading" :rows="6" animated />
        <DataTable v-else :data="builds">
          <el-table-column prop="triggerType" label="触发" width="110">
            <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.triggerType || '—' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="refName" label="引用" width="140">
            <template #default="{ row }"><span class="mono">{{ row.refName || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="commitSha" label="Commit" width="120">
            <template #default="{ row }"><span class="mono">{{ (row.commitSha || '').slice(0, 8) || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="commitMessage" label="提交信息" min-width="220" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }"><StatusBadge :status="row.status" :text="row.status" /></template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" min-width="140">
            <template #default="{ row }"><span class="mono">{{ row.createTime || '—' }}</span></template>
          </el-table-column>
        </DataTable>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" title="绑定代码仓库" width="520px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="平台" required>
          <el-select v-model="form.provider" style="width: 100%">
            <el-option v-for="p in ['github', 'gitlab', 'gitee']" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库地址" required>
          <el-input v-model="form.repoRef" placeholder="owner/repo" />
        </el-form-item>
        <el-form-item label="分支">
          <el-input v-model="form.branch" placeholder="main" />
        </el-form-item>
        <el-form-item label="环境">
          <el-select v-model="form.environment" style="width: 100%">
            <el-option v-for="e in ['dev', 'staging', 'prod']" :key="e" :label="e" :value="e" />
          </el-select>
        </el-form-item>
        <el-form-item label="部署脚本">
          <el-input v-model="form.deployScriptPath" placeholder="scripts/deploy.sh" />
        </el-form-item>
        <el-form-item label="Access Token">
          <el-input v-model="token" type="password" show-password />
        </el-form-item>
        <el-form-item label="自动触发">
          <el-switch v-model="autoTrigger" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">绑定</el-button>
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
import { opsGitApi, type OpsRepoEntity, type OpsBuildEntity } from '@/api/ops.module'

const queryClient = useQueryClient()
const tab = ref('repos')
const repos = ref<OpsRepoEntity[]>([])
const builds = ref<OpsBuildEntity[]>([])
const reposLoading = ref(false)
const buildsLoading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const form = ref<Record<string, unknown>>({})
const token = ref('')
const autoTrigger = ref(true)

async function loadRepos() {
  reposLoading.value = true
  try {
    repos.value = (await opsGitApi.repos()) || []
  } finally {
    reposLoading.value = false
  }
}
async function loadBuilds() {
  buildsLoading.value = true
  try {
    builds.value = (await opsGitApi.builds()) || []
  } finally {
    buildsLoading.value = false
  }
}
loadRepos()
loadBuilds()

function openCreate() {
  form.value = { provider: 'github', repoRef: '', branch: 'main', environment: 'dev', deployScriptPath: '' }
  token.value = ''
  autoTrigger.value = true
  dialogVisible.value = true
}
async function save() {
  if (!form.value.repoRef) {
    ElMessage.warning('仓库地址必填')
    return
  }
  saving.value = true
  try {
    await opsGitApi.bind({
      ...form.value,
      autoTrigger: autoTrigger.value,
      token: token.value,
    })
    ElMessage.success('绑定成功')
    dialogVisible.value = false
    queryClient.invalidateQueries({ queryKey: ['ops-git'] })
    await loadRepos()
  } finally {
    saving.value = false
  }
}
async function trigger(row: OpsRepoEntity) {
  await opsGitApi.trigger(row.id)
  ElMessage.success('已触发构建')
  await loadBuilds()
}
async function syncRepo(row: OpsRepoEntity) {
  await opsGitApi.syncRepo(row.id)
  ElMessage.success('已触发同步')
  await loadRepos()
}
async function unbind(row: OpsRepoEntity) {
  await ElMessageBox.confirm(`确认解绑仓库 ${row.repoRef} ？`, '提示', { type: 'warning' })
  await opsGitApi.unbind(row.id)
  ElMessage.success('已解绑')
  await loadRepos()
}
</script>

<style scoped>
.page { padding: 24px; }
.git-tabs { margin-top: 4px; }
.mono { font-family: var(--td-font-mono); }
</style>