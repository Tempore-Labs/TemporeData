<template>
  <SearchTable
    title="全局变量"
    :loading="crud.table.loading"
    :total="crud.table.total"
    v-model:current-page="crud.table.currentPage"
    v-model:page-size="crud.table.pageSize"
    add-text="新增变量"
    @add="crud.dialog.openAdd"
  >
    <template #default>
      <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe style="width: 100%">
        <el-table-column prop="varKey" label="变量名" min-width="150" />
        <el-table-column prop="varValue" label="变量值" min-width="200" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
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
          <el-form-item label="变量名" prop="varKey">
            <el-input v-model="crud.dialog.form.varKey" placeholder="请输入变量名" />
          </el-form-item>
          <el-form-item label="变量值" prop="varValue">
            <el-input v-model="crud.dialog.form.varValue" placeholder="请输入变量值" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="crud.dialog.form.description" placeholder="请输入描述" />
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
import { globalvarApi } from '@/api/modules/globalvar'
import { useCRUD } from '@/composables/useCRUD'
import SearchTable from '@/components/SearchTable.vue'
import { Edit, Delete } from '@element-plus/icons-vue'

const defaultForm = () => ({
  varKey: '',
  varValue: '',
  description: ''
})

const rules = {
  varKey: [{ required: true, message: '请输入变量名', trigger: 'blur' }],
  varValue: [{ required: true, message: '请输入变量值', trigger: 'blur' }]
}

const crud = useCRUD(globalvarApi, defaultForm, rules, { entityName: '全局变量' })
onMounted(() => { crud.table.fetch() })
</script>