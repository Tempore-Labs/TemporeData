<template>
  <SearchTable
    title="免密登录"
    :loading="crud.table.loading"
    :total="crud.table.total"
    v-model:current-page="crud.table.currentPage"
    v-model:page-size="crud.table.pageSize"
    add-text="新增登录配置"
    @add="crud.dialog.openAdd"
  >
    <template #default>
      <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe style="width: 100%">
        <el-table-column prop="loginType" label="登录方式" width="150">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.loginType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" effect="light">{{ row.status === 'ENABLED' ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="crud.dialog.openEdit(row)"><el-icon><Edit /></el-icon>编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="crud.handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link><el-icon><Delete /></el-icon>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <template #dialog>
      <el-dialog v-model="crud.dialog.visible" :title="crud.dialog.isEdit ? '编辑' : '新增'" width="520px" :close-on-click-modal="false" destroy-on-close>
        <el-form ref="crud.dialog.formRef" :model="crud.dialog.form" :rules="crud.dialog.rules" label-width="100px" label-position="right">
          <el-form-item label="登录方式" prop="loginType">
            <el-select v-model="crud.dialog.form.loginType" placeholder="请选择登录方式" style="width: 100%">
              <el-option label="短信" value="SMS" />
              <el-option label="邮箱" value="EMAIL" />
              <el-option label="OAuth" value="OAUTH" />
              <el-option label="LDAP" value="LDAP" />
            </el-select>
          </el-form-item>
          <el-form-item label="配置" prop="config">
            <el-input v-model="crud.dialog.form.config" type="textarea" :rows="4" placeholder="请输入配置" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="crud.dialog.close()">取消</el-button>
          <el-button type="primary" :loading="crud.dialog.submitting" @click="crud.handleSubmit">确定</el-button>
        </template>
      </el-dialog>
    </template>
  </SearchTable>
</template>

<script setup>
import { onMounted } from 'vue'
import { passwordlessApi } from '@/api/modules/passwordless'
import { useCRUD } from '@/composables/useCRUD'
import SearchTable from '@/components/SearchTable.vue'
import { Edit, Delete } from '@element-plus/icons-vue'

const defaultForm = () => ({
  loginType: '',
  config: ''
})

const rules = {
  loginType: [{ required: true, message: '请选择登录方式', trigger: 'change' }],
  config: [{ required: true, message: '请输入配置', trigger: 'blur' }]
}

const crud = useCRUD(passwordlessApi, defaultForm, rules, { entityName: '登录配置' })
onMounted(() => { crud.table.fetch() })
</script>