<template>
  <SearchTable
    title="敏感数据"
    :loading="crud.table.loading"
    :total="crud.table.total"
    v-model:current-page="crud.table.currentPage"
    v-model:page-size="crud.table.pageSize"
    add-text="新增敏感数据"
    @add="crud.dialog.openAdd"
  >
    <template #default>
      <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe style="width: 100%">
        <el-table-column prop="tableName" label="表名" min-width="150" />
        <el-table-column prop="columnName" label="列名" min-width="150" />
        <el-table-column prop="sensitiveType" label="敏感类型" width="120">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.sensitiveType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maskRule" label="脱敏规则" width="120">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.maskRule }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="等级" width="80">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)" effect="light">{{ row.level }}</el-tag>
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
          <el-form-item label="数据源ID" prop="datasourceId">
            <el-input v-model="crud.dialog.form.datasourceId" placeholder="请输入数据源ID" />
          </el-form-item>
          <el-form-item label="表名" prop="tableName">
            <el-input v-model="crud.dialog.form.tableName" placeholder="请输入表名" />
          </el-form-item>
          <el-form-item label="列名" prop="columnName">
            <el-input v-model="crud.dialog.form.columnName" placeholder="请输入列名" />
          </el-form-item>
          <el-form-item label="敏感类型" prop="sensitiveType">
            <el-select v-model="crud.dialog.form.sensitiveType" placeholder="请选择敏感类型" style="width: 100%">
              <el-option label="PHONE" value="PHONE" />
              <el-option label="EMAIL" value="EMAIL" />
              <el-option label="ID_CARD" value="ID_CARD" />
              <el-option label="BANK_CARD" value="BANK_CARD" />
              <el-option label="NAME" value="NAME" />
              <el-option label="ADDRESS" value="ADDRESS" />
              <el-option label="CUSTOM" value="CUSTOM" />
            </el-select>
          </el-form-item>
          <el-form-item label="脱敏规则" prop="maskRule">
            <el-select v-model="crud.dialog.form.maskRule" placeholder="请选择脱敏规则" style="width: 100%">
              <el-option label="FULL_MASK" value="FULL_MASK" />
              <el-option label="PARTIAL_MASK" value="PARTIAL_MASK" />
              <el-option label="HASH" value="HASH" />
              <el-option label="REPLACE" value="REPLACE" />
            </el-select>
          </el-form-item>
          <el-form-item label="等级" prop="level">
            <el-select v-model="crud.dialog.form.level" placeholder="请选择等级" style="width: 100%">
              <el-option label="L1" value="L1" />
              <el-option label="L2" value="L2" />
              <el-option label="L3" value="L3" />
              <el-option label="L4" value="L4" />
            </el-select>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="crud.dialog.form.description" type="textarea" :rows="3" placeholder="请输入描述" />
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
import { sensitiveApi } from '@/api/modules/sensitive'
import { useCRUD } from '@/composables/useCRUD'
import SearchTable from '@/components/SearchTable.vue'
import { Edit, Delete } from '@element-plus/icons-vue'

const defaultForm = () => ({
  datasourceId: '',
  tableName: '',
  columnName: '',
  sensitiveType: '',
  maskRule: '',
  level: '',
  description: ''
})

const rules = {
  datasourceId: [{ required: true, message: '请输入数据源ID', trigger: 'blur' }],
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  columnName: [{ required: true, message: '请输入列名', trigger: 'blur' }],
  sensitiveType: [{ required: true, message: '请选择敏感类型', trigger: 'change' }],
  maskRule: [{ required: true, message: '请选择脱敏规则', trigger: 'change' }],
  level: [{ required: true, message: '请选择等级', trigger: 'change' }]
}

const crud = useCRUD(sensitiveApi, defaultForm, rules, { entityName: '敏感数据' })

function levelTag(level) {
  const map = { L1: 'info', L2: 'warning', L3: '', L4: 'danger' }
  return map[level] || 'info'
}

onMounted(() => { crud.table.fetch() })
</script>