<template>
  <div class="preference-page">
    <div class="page-header">
      <h2 class="page-title">偏好设置</h2>
    </div>
    <el-card shadow="never" v-loading="loading">
      <el-form ref="formRef" :model="form" label-width="100px" style="max-width: 520px">
        <el-form-item v-for="(item, index) in form.items" :key="index" :label="'配置 ' + (index + 1)" :prop="'items.' + index + '.prefKey'" :rules="[{ required: true, message: '请输入键', trigger: 'blur' }]">
          <el-input v-model="item.prefKey" placeholder="键" style="width: 200px; margin-right: 8px" />
          <el-input v-model="item.prefValue" placeholder="值" style="width: 200px; margin-right: 8px" />
          <el-button type="danger" :icon="Delete" circle size="small" @click="removeItem(index)" />
        </el-form-item>
        <el-form-item>
          <el-button @click="addItem">添加配置</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { preferenceApi } from '@/api/modules/preference'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'

const loading = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  items: []
})

function addItem() {
  form.items.push({ prefKey: '', prefValue: '' })
}

function removeItem(index) {
  form.items.splice(index, 1)
}

async function handleSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const data = {}
    form.items.forEach(item => {
      if (item.prefKey) data[item.prefKey] = item.prefValue
    })
    await preferenceApi.batchSave(data)
    ElMessage.success('偏好设置已保存')
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const prefs = await preferenceApi.list()
    if (Array.isArray(prefs)) {
      form.items = prefs.map(p => ({ prefKey: p.prefKey, prefValue: p.prefValue }))
    } else if (prefs && typeof prefs === 'object') {
      form.items = Object.entries(prefs).map(([k, v]) => ({ prefKey: k, prefValue: v }))
    }
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.preference-page {
  padding: 0;
}
.page-header {
  margin-bottom: 20px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}
</style>