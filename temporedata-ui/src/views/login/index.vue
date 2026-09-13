<template>
  <div class="login-page">
    <!-- Left brand panel -->
    <div class="brand-panel">
      <div class="grid-overlay" />
      <div class="orb orb-a" />
      <div class="orb orb-b" />

      <div class="brand-logo">
        <div class="logo-mark">
          <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
            <path d="M11 2L18 6.5V15.5L11 20L4 15.5V6.5L11 2Z" fill="white" fill-opacity="0.3" stroke="white" stroke-width="1.5" />
            <path d="M11 6L15 8.5V13.5L11 16L7 13.5V8.5L11 6Z" fill="white" />
          </svg>
        </div>
        <span class="brand-text">TemporeData</span>
      </div>

      <div class="brand-main">
        <div class="brand-tag">数据智能平台 v3.0</div>
        <h1 class="brand-headline">一站式数据<br />开发与治理平台</h1>
        <p class="brand-desc">
          集数据集成、开发、资产管理、血缘分析、质量监控于一体，助力企业构建现代化数据基础设施。
        </p>

        <div class="feature-grid">
          <div v-for="f in features" :key="f.label" class="feature-pill">
            <el-icon :size="18" class="feature-icon"><component :is="f.icon" /></el-icon>
            <div>
              <div class="feature-label">{{ f.label }}</div>
              <div class="feature-desc">{{ f.desc }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="brand-foot">© 2026 Tempore Labs · 企业数据中台解决方案</div>
    </div>

    <!-- Right form panel -->
    <div class="form-panel">
      <div class="form-box">
        <div class="mobile-logo">
          <div class="logo-mark-sm">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M8 2L13 5V11L8 14L3 11V5L8 2Z" fill="white" fill-opacity="0.3" stroke="white" stroke-width="1.2" />
              <path d="M8 5L11 7V10L8 12L5 10V7L8 5Z" fill="white" />
            </svg>
          </div>
          <span>TemporeData</span>
        </div>

        <h2 class="form-title">欢迎回来</h2>
        <p class="form-sub">登录至 TemporeData 控制台</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="onSubmit">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="remember">记住我</el-checkbox>
            <el-link type="primary" :underline="false">忘记密码?</el-link>
          </div>

          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            native-type="submit"
            @click.prevent="onSubmit"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form>

        <div class="form-tip">演示账号: admin / admin123</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const remember = ref(true)

const form = reactive({ username: 'admin', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const features = [
  { icon: 'Share', label: '数据血缘', desc: '全链路字段级血缘追踪' },
  { icon: 'Coin', label: '数据资产', desc: '统一数据目录与元数据' },
  { icon: 'Aim', label: '质量管理', desc: '多维度数据质量监控' },
  { icon: 'Edit', label: 'SQL 开发', desc: '在线编辑与血缘解析' },
]

async function onSubmit() {
  const ok = await formRef.value?.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    await auth.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || err?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
}

/* Left panel */
.brand-panel {
  position: relative;
  flex: 0 0 55%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(135deg, #1a6ff5 0%, #0d47a1 100%);
}
.grid-overlay {
  position: absolute;
  inset: 0;
  opacity: 0.1;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.3) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.3) 1px, transparent 1px);
  background-size: 40px 40px;
}
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(48px);
}
.orb-a {
  top: 64px;
  right: 64px;
  width: 256px;
  height: 256px;
  background: rgba(255, 255, 255, 0.05);
}
.orb-b {
  bottom: 96px;
  left: 32px;
  width: 192px;
  height: 192px;
  background: rgba(96, 165, 250, 0.1);
}

.brand-logo {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
}
.logo-mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand-text {
  font-size: 18px;
  font-weight: 600;
}

.brand-main {
  position: relative;
}
.brand-tag {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 16px;
}
.brand-headline {
  font-size: 38px;
  font-weight: 700;
  line-height: 1.3;
  margin: 0 0 16px;
}
.brand-desc {
  max-width: 380px;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.7);
}
.feature-grid {
  margin-top: 32px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  max-width: 520px;
}
.feature-pill {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(4px);
}
.feature-icon {
  margin-top: 2px;
  color: rgba(255, 255, 255, 0.85);
}
.feature-label {
  font-size: 13px;
  font-weight: 600;
}
.feature-desc {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 2px;
}
.brand-foot {
  position: relative;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

/* Right panel */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f8fa;
  padding: 32px;
}
.form-box {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 1px 4px rgba(0, 0, 0, 0.04);
}
.mobile-logo {
  display: none;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  font-weight: 600;
  color: var(--td-text-1);
}
.logo-mark-sm {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--td-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}
.form-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--td-text-1);
  margin: 0 0 4px;
}
.form-sub {
  font-size: 13px;
  color: var(--td-text-3);
  margin: 0 0 24px;
}
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.submit-btn {
  width: 100%;
  height: 40px;
  font-weight: 600;
}
.form-tip {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--td-border-light);
  text-align: center;
  font-size: 12px;
  color: var(--td-text-4);
}

@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }
  .mobile-logo {
    display: flex;
  }
}
</style>
