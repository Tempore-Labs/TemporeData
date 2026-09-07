<template>
  <SearchTable
    title="通知配置"
    :loading="crud.table.loading"
    :total="crud.table.total"
    v-model:current-page="crud.table.currentPage"
    v-model:page-size="crud.table.pageSize"
    add-text="新增通知"
    @add="crud.dialog.openAdd"
  >
    <template #default>
      <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe style="width: 100%">
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="channelType" label="渠道类型" width="120">
          <template #default="{ row }">
            <el-tag :type="channelTypeTag(row.channelType)" effect="light">{{ row.channelType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" effect="light">
              {{ row.enabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
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
          <el-form-item label="名称" prop="name">
            <el-input v-model="crud.dialog.form.name" placeholder="请输入通知名称" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="渠道类型" prop="channelType">
            <el-select v-model="crud.dialog.form.channelType" placeholder="请选择渠道类型" style="width: 100%">
              <el-option label="EMAIL" value="EMAIL" />
              <el-option label="SMS" value="SMS" />
              <el-option label="WEBHOOK" value="WEBHOOK" />
            </el-select>
          </el-form-item>
          <el-form-item label="配置" prop="config">
            <el-input v-model="crud.dialog.form.config" type="textarea" :rows="4" placeholder="请输入配置信息" />
          </el-form-item>
          <el-form-item label="启用" prop="enabled">
            <el-switch v-model="crud.dialog.form.enabled" :active-value="1" :inactive-value="0" />
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
import { notifyApi } from '@/api/modules/notify'
import { useCRUD } from '@/composables/useCRUD'
import SearchTable from '@/components/SearchTable.vue'
import { Edit, Delete } from '@element-plus/icons-vue'

const defaultForm = () => ({
  name: '',
  channelType: '',
  config: '',
  enabled: 0
})

const rules = {
  name: [{ required: true, message: '请输入通知名称', trigger: 'blur' }],
  channelType: [{ required: true, message: '请选择渠道类型', trigger: 'change' }],
  config: [{ required: true, message: '请输入配置信息', trigger: 'blur' }]
}

const crud = useCRUD(notifyApi, defaultForm, rules, { entityName: '通知' })

function channelTypeTag(type) {
  const map = { EMAIL: '', SMS: 'warning', WEBHOOK: 'success' }
  return map[type] || 'info'
}

onMounted(() => { crud.table.fetch() })
</script>