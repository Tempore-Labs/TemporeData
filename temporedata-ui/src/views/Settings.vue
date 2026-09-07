<template>
  <div class="settings">
    <div class="page-header">
      <h2>系统设置</h2>
      <p>TemporeData 大数据平台 · 平台配置与管理</p>
    </div>

    <div class="settings-grid">
      <!-- 系统信息 -->
      <el-card class="settings-card">
        <template #header>
          <div class="card-header">
            <el-icon><InfoFilled /></el-icon>
            <span>系统信息</span>
          </div>
        </template>
        <el-descriptions :column="1" border size="small" v-loading="sysLoading">
          <el-descriptions-item label="系统版本">
            {{ sysInfo.version || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="构建时间">
            {{ sysInfo.buildTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Java 版本">
            {{ sysInfo.javaVersion || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="数据库类型">
            {{ sysInfo.databaseType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="运行时长">
            {{ sysInfo.uptime || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

    <!-- 平台设置 -->
    <el-card class="settings-card platform-settings">
      <template #header>
        <div class="card-header">
          <el-icon><Setting /></el-icon>
          <span>平台设置</span>
        </div>
      </template>
      <el-form
        ref="formRef"
        :model="platformForm"
        label-width="110px"
        class="settings-form"
        v-loading="platformLoading"
      >
        <el-form-item label="站点名称">
          <el-input v-model="platformForm.siteName" placeholder="TemporeData 大数据平台" />
        </el-form-item>
        <el-form-item label="平台 Logo">
          <el-input v-model="platformForm.logo" placeholder="请输入 Logo URL" />
        </el-form-item>
        <el-form-item label="平台描述">
          <el-input
            v-model="platformForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入平台描述"
          />
        </el-form-item>
        <el-divider />
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">
            保存设置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, InfoFilled } from '@element-plus/icons-vue'
import { settingsApi } from '@/api/modules/settings'

const formRef = ref(null)
const saving = ref(false)
const sysLoading = ref(false)
const platformLoading = ref(false)

const sysInfo = reactive({
  version: '',
  buildTime: '',
  javaVersion: '',
  databaseType: '',
  uptime: ''
})

const platformForm = reactive({
  siteName: 'TemporeData 大数据平台',
  logo: '',
  description: ''
})

onMounted(async () => {
  // Load platform settings from backend
  await loadPlatformSettings()
})

async function loadPlatformSettings() {
  platformLoading.value = true
  sysLoading.value = true
  try {
    const group = await settingsApi.getGroup('platform')
    if (group) {
      platformForm.siteName = group['site.name'] || 'TemporeData 大数据平台'
      platformForm.logo = group['site.logo'] || ''
      platformForm.description = group['site.description'] || ''
    }
    const sysGroup = await settingsApi.getGroup('system')
    if (sysGroup) {
      sysInfo.version = sysGroup['system.version'] || 'v2.4.1'
      sysInfo.javaVersion = sysGroup['system.javaVersion'] || 'OpenJDK 17'
      sysInfo.databaseType = sysGroup['system.databaseType'] || 'MySQL'
    }
  } catch {
    // Use defaults
    sysInfo.version = 'v2.4.1'
    sysInfo.buildTime = '2026-07-28 14:30:00'
    sysInfo.javaVersion = 'OpenJDK 17.0.9'
    sysInfo.databaseType = 'MySQL'
    sysInfo.uptime = '-'
  } finally {
    platformLoading.value = false
    sysLoading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await settingsApi.saveGroup('platform', {
      'site.name': platformForm.siteName,
      'site.logo': platformForm.logo,
      'site.description': platformForm.description
    })
    ElMessage.success('平台设置保存成功')
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '请稍后重试'))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.settings {
  padding: 0;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.page-header p {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.settings-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.settings-card {
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.settings-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.card-header .el-icon {
  font-size: 18px;
  color: #409eff;
}

.settings-card :deep(.el-card__body) {
  padding: 20px;
}

.platform-settings {
  grid-column: 1 / -1;
}

.settings-form {
  max-width: 560px;
}

@media (max-width: 768px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>