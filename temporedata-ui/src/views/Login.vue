<template>
  <div class="login-page">
    <div class="login-left">
      <div class="login-left-content">
        <div class="login-brand">
          <svg class="login-logo" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
          </svg>
          <h1>TemporeData</h1>
        </div>
        <p class="login-desc">统一的数据开发、资产、服务与治理工作台</p>
        <div class="login-features">
          <div class="feature-item">
            <el-icon><DataBoard /></el-icon>
            <span>数据可视化大屏</span>
          </div>
          <div class="feature-item">
            <el-icon><Connection /></el-icon>
            <span>多源数据同步</span>
          </div>
          <div class="feature-item">
            <el-icon><Monitor /></el-icon>
            <span>智能质量稽核</span>
          </div>
          <div class="feature-item">
            <el-icon><Lock /></el-icon>
            <span>数据安全治理</span>
          </div>
        </div>
      </div>
    </div>
    <div class="login-right">
      <el-card class="login-card" shadow="never">
        <h2 class="login-title">欢迎登录</h2>
        <p class="login-subtitle">请输入您的账号密码</p>
        <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleLogin" style="width:100%">
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>
        <p class="login-error" v-if="errorMsg">{{ errorMsg }}</p>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { authApi } from '@/api/modules/auth'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)
const errorMsg = ref('')

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await authApi.login(form.username, form.password)
    authStore.setAuth(data.token, data.user)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (err) {
    errorMsg.value = err.response?.data?.msg || err.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
}
.login-left {
  flex: 0 0 55%;
  background: linear-gradient(135deg, #1a6ff5 0%, #0d47a1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #fff;
}
.login-left-content {
  max-width: 480px;
}
.login-brand {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.login-logo {
  width: 48px;
  height: 48px;
}
.login-brand h1 {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
}
.login-desc {
  font-size: 15px;
  line-height: 1.6;
  opacity: 0.9;
  margin-bottom: 40px;
}
.login-features {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  opacity: 0.85;
}
.login-right {
  flex: 0 0 45%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f7f7;
}
.login-card {
  width: 400px;
  padding: 20px;
  border: none;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06);
}
.login-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 8px;
  text-align: center;
}
.login-subtitle {
  font-size: 14px;
  color: #6b7280;
  text-align: center;
  margin-bottom: 32px;
}
.login-error {
  color: #ef4444;
  font-size: 13px;
  text-align: center;
  margin-top: -8px;
}
@media (max-width: 768px) {
  .login-left { display: none; }
  .login-right { flex: 1; }
  .login-card { width: 90%; }
}
</style>