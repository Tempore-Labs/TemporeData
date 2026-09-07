<template>
  <div class="profile-page">
    <div class="page-header">
      <h2 class="page-title">个人中心</h2>
    </div>
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基础信息" name="info">
          <el-form :model="profileForm" label-width="100px" v-loading="infoLoading" style="max-width: 520px">
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="用户名"><el-input :model-value="userInfo.username" disabled /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="租户ID"><el-input :model-value="userInfo.tenantId" disabled /></el-form-item></el-col>
            </el-row>
            <el-form-item label="角色">
              <el-tag v-for="role in userInfo.roles" :key="role" style="margin-right: 4px" effect="light">{{ role }}</el-tag>
              <span v-if="!userInfo.roles || userInfo.roles.length === 0">-</span>
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="50" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" maxlength="20" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" maxlength="100" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="profileSaving" @click="handleSaveProfile">保存资料</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" style="max-width: 420px">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordSubmitting" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改手机" name="phone">
          <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules" label-width="100px" style="max-width: 420px">
            <el-form-item label="新手机号" prop="value">
              <el-input v-model="phoneForm.value" placeholder="请输入新手机号" maxlength="20" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="phoneSubmitting" @click="handleChangePhone">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改邮箱" name="email">
          <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-width="100px" style="max-width: 420px">
            <el-form-item label="新邮箱" prop="value">
              <el-input v-model="emailForm.value" placeholder="请输入新邮箱" maxlength="100" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="emailSubmitting" @click="handleChangeEmail">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="偏好设置" name="preference">
          <el-form ref="prefFormRef" :model="prefForm" label-width="100px" style="max-width: 520px" v-loading="prefLoading">
            <el-form-item v-for="(item, index) in prefForm.items" :key="index" :label="'配置 ' + (index + 1)">
              <el-input v-model="item.key" placeholder="键" style="width: 200px; margin-right: 8px" />
              <el-input v-model="item.value" placeholder="值" style="width: 200px" />
            </el-form-item>
            <el-form-item>
              <el-button @click="addPrefItem">添加配置</el-button>
              <el-button type="primary" :loading="prefSaving" @click="handleSavePreferences">保存偏好</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="message-link">
        <el-link type="primary" :href="`#/message`">前往消息中心</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { userApi } from '@/api/modules/user'
import { preferenceApi } from '@/api/modules/preference'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const activeTab = ref('info')

// 基础信息
const infoLoading = ref(false)
const profileSaving = ref(false)
const userInfo = reactive({ username: '', tenantId: '', roles: [] })
const profileForm = reactive({ nickname: '', phone: '', email: '' })

// 修改密码
const passwordFormRef = ref(null)
const passwordSubmitting = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== passwordForm.newPassword) callback(new Error('两次输入的密码不一致'))
  else callback()
}
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码长度至少6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }, { validator: validateConfirmPassword, trigger: 'blur' }]
}

// 修改手机 / 邮箱
const phoneFormRef = ref(null)
const emailFormRef = ref(null)
const phoneSubmitting = ref(false)
const emailSubmitting = ref(false)
const phoneForm = reactive({ value: '' })
const emailForm = reactive({ value: '' })
const phoneRules = { value: [{ required: true, message: '请输入新手机号', trigger: 'blur' }] }
const emailRules = { value: [{ required: true, message: '请输入新邮箱', trigger: 'blur' }] }

// 偏好设置
const prefFormRef = ref(null)
const prefLoading = ref(false)
const prefSaving = ref(false)
const prefForm = reactive({ items: [] })

function addPrefItem() { prefForm.items.push({ key: '', value: '' }) }

async function handleSaveProfile() {
  profileSaving.value = true
  try {
    const updated = await userApi.updateProfile({ ...profileForm })
    Object.assign(userInfo, updated || {})
    if (authStore.user) Object.assign(authStore.user, updated || {})
    ElMessage.success('资料已保存')
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    profileSaving.value = false
  }
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return
  try { await passwordFormRef.value.validate() } catch { return }
  passwordSubmitting.value = true
  try {
    await userApi.changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码修改成功')
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || err.message || '修改密码失败')
  } finally {
    passwordSubmitting.value = false
  }
}

async function handleChangePhone() {
  if (!phoneFormRef.value) return
  try { await phoneFormRef.value.validate() } catch { return }
  phoneSubmitting.value = true
  try {
    const updated = await userApi.changePhone({ value: phoneForm.value })
    if (updated) profileForm.phone = updated.phone
    ElMessage.success('手机号已更新')
    phoneForm.value = ''
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || err.message || '更新失败')
  } finally {
    phoneSubmitting.value = false
  }
}

async function handleChangeEmail() {
  if (!emailFormRef.value) return
  try { await emailFormRef.value.validate() } catch { return }
  emailSubmitting.value = true
  try {
    const updated = await userApi.changeEmail({ value: emailForm.value })
    if (updated) profileForm.email = updated.email
    ElMessage.success('邮箱已更新')
    emailForm.value = ''
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || err.message || '更新失败')
  } finally {
    emailSubmitting.value = false
  }
}

async function handleSavePreferences() {
  prefSaving.value = true
  try {
    const data = {}
    prefForm.items.forEach(item => { if (item.key) data[item.key] = item.value })
    await preferenceApi.batchSave(data)
    ElMessage.success('偏好设置已保存')
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    prefSaving.value = false
  }
}

onMounted(async () => {
  infoLoading.value = true
  try {
    const data = await userApi.me()
    Object.assign(userInfo, data)
    Object.assign(profileForm, { nickname: data.nickname || '', phone: data.phone || '', email: data.email || '' })
  } catch {
    if (authStore.user) Object.assign(userInfo, authStore.user)
  } finally {
    infoLoading.value = false
  }
  prefLoading.value = true
  try {
    const prefs = await preferenceApi.list()
    if (Array.isArray(prefs)) prefForm.items = prefs.map(p => ({ key: p.prefKey, value: p.prefValue }))
    else if (prefs && typeof prefs === 'object') prefForm.items = Object.entries(prefs).map(([k, v]) => ({ key: k, value: v }))
  } catch {
    // ignore
  } finally {
    prefLoading.value = false
  }
})
</script>

<style scoped>
.profile-page { padding: 0; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.message-link { margin-top: 16px; text-align: right; }
</style>