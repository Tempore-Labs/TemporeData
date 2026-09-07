<template>
  <div class="ops-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">Ops 集成</h2>
        <p class="page-desc">接入 GitHub / GitLab，提交推送自动触发部署到目标环境（与多环境部署串联）</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openBind">绑定仓库</el-button>
    </div>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <el-tab-pane label="仓库" name="repos">
        <el-card shadow="never">
          <el-table :data="repos" v-loading="loading" stripe>
            <el-table-column label="供应商" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ providerName(row.provider) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="repoRef" label="仓库" min-width="180" />
            <el-table-column prop="branch" label="分支" width="90" />
            <el-table-column label="自动触发" width="100">
              <template #default="{ row }">{{ row.autoTrigger ? '开' : '关' }}</template>
            </el-table-column>
            <el-table-column prop="environment" label="环境" width="90" />
            <el-table-column prop="deployScriptPath" label="部署脚本" min-width="150" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" align="center">
              <template #default="{ row }">
                <el-button size="small" link type="primary" @click="doSync(row)">测试连接</el-button>
                <el-button size="small" link type="success" @click="doTrigger(row)" :loading="row._triggering">触发</el-button>
                <el-popconfirm title="解绑该仓库？" @confirm="doUnbind(row)">
                  <template #reference>
                    <el-button size="small" link type="danger">解绑</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="构建记录" name="builds">
        <el-card shadow="never">
          <div class="build-toolbar">
            <el-select v-model="buildRepoFilter" placeholder="按仓库筛选" clearable style="width: 220px" @change="loadBuilds">
              <el-option v-for="r in repos" :key="r.id" :label="r.repoRef" :value="r.id" />
            </el-select>
          </div>
          <el-table :data="builds" v-loading="loading" stripe>
            <el-table-column prop="triggerType" label="触发" width="90" />
            <el-table-column prop="repoId" label="仓库ID" min-width="180" show-overflow-tooltip />
            <el-table-column prop="commitSha" label="Commit" width="120" show-overflow-tooltip />
            <el-table-column prop="commitMessage" label="提交信息" min-width="200" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="buildStatusTag(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="pipelineRef" label="流水线" min-width="130" />
            <el-table-column prop="createTime" label="时间" min-width="150" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 绑定弹窗 -->
    <el-dialog v-model="bindVisible" title="绑定 Git 仓库" width="520px" :close-on-click-modal="false">
      <el-form label-width="110px">
        <el-form-item label="供应商">
          <el-select v-model="form.provider" style="width: 100%">
            <el-option v-for="p in providers" :key="p.providerId" :label="p.name" :value="p.providerId" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库引用">
          <el-input v-model="form.repoRef" placeholder="owner/repo 或 project/path" />
        </el-form-item>
        <el-form-item label="分支">
          <el-input v-model="form.branch" placeholder="main" />
        </el-form-item>
        <el-form-item label="目标环境">
          <el-select v-model="form.environment" style="width: 100%">
            <el-option label="DEV" value="DEV" />
            <el-option label="TEST" value="TEST" />
            <el-option label="PROD" value="PROD" />
          </el-select>
        </el-form-item>
        <el-form-item label="部署脚本路径">
          <el-input v-model="form.deployScriptPath" placeholder="如 deploy/workflow.json" />
        </el-form-item>
        <el-form-item label="自动触发">
          <el-switch v-model="form.autoTrigger" />
          <span class="bind-hint" v-if="form.autoTrigger">启用后将注册推送 Webhook</span>
        </el-form-item>
        <el-form-item label="访问令牌(可选)">
          <el-input v-model="form.token" type="password" placeholder="云平台已配置则留空" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="binding" :disabled="!form.provider || !form.repoRef" @click="doBind">绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { opsApi } from '@/api/modules/ops'

const tab = ref('repos')
const loading = ref(false)
const repos = ref([])
const builds = ref([])
const providers = ref([])
const buildRepoFilter = ref('')

const bindVisible = ref(false)
const binding = ref(false)
const form = ref({
  provider: 'github', repoRef: '', branch: 'main', environment: 'DEV',
  deployScriptPath: '', autoTrigger: false, token: ''
})

function providerName(id) {
  const p = providers.value.find(x => x.providerId === id)
  return p ? p.name : id
}
function buildStatusTag(s) {
  const map = { TRIGGERED: 'warning', SUCCESS: 'success', FAILED: 'danger' }
  return map[s] || 'info'
}

async function loadAll() {
  loading.value = true
  try {
    const [pr, rp] = await Promise.all([opsApi.providers(), opsApi.repos()])
    providers.value = pr || []
    repos.value = rp || []
  } catch (e) {
    ElMessage.error('加载失败：' + (e.message || ''))
  } finally { loading.value = false }
}

async function loadBuilds() {
  loading.value = true
  try {
    builds.value = await opsApi.builds(buildRepoFilter.value || undefined) || []
  } catch (e) {
    ElMessage.error('加载构建记录失败')
  } finally { loading.value = false }
}

function onTabChange(name) {
  if (name === 'builds') loadBuilds()
}

function openBind() {
  form.value = { provider: 'github', repoRef: '', branch: 'main', environment: 'DEV', deployScriptPath: '', autoTrigger: false, token: '' }
  bindVisible.value = true
}

async function doBind() {
  binding.value = true
  try {
    await opsApi.bind(form.value)
    ElMessage.success('绑定成功')
    bindVisible.value = false
    await loadAll()
  } catch (e) {
    ElMessage.error(e.message || '绑定失败')
  } finally { binding.value = false }
}

async function doSync(row) {
  try {
    const res = await opsApi.sync(row.id)
    ElMessage.info((res.status || 'NA') + ' - ' + (res.message || '') + (res.latestCommit ? '；' + res.latestCommit : ''))
  } catch (e) {
    ElMessage.error(e.message || '测试失败')
  }
}

async function doTrigger(row) {
  row._triggering = true
  try {
    const b = await opsApi.trigger(row.id)
    ElMessage.success(`已触发构建，状态：${b.status}`)
    await loadBuilds()
  } catch (e) {
    ElMessage.error(e.message || '触发失败')
  } finally { row._triggering = false }
}

async function doUnbind(row) {
  try {
    await opsApi.unbind(row.id)
    ElMessage.success('已解绑')
    await loadAll()
  } catch (e) {
    ElMessage.error(e.message || '解绑失败')
  }
}

onMounted(loadAll)
</script>

<style scoped>
.ops-page { max-width: 1280px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.page-desc { font-size: 13px; color: #9ca3af; margin: 4px 0 0; }
.build-toolbar { margin-bottom: 12px; }
.bind-hint { font-size: 12px; color: #909399; margin-left: 10px; }
</style>