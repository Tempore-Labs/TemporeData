<template>
  <SearchTable
    title="发布审批"
    :loading="crud.table.loading"
    :total="crud.table.total"
    v-model:current-page="crud.table.currentPage"
    v-model:page-size="crud.table.pageSize"
    add-text="新增审批"
    @add="crud.dialog.openAdd"
  >
    <template #default>
      <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="150" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantId" label="申请人" width="120" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="crud.dialog.openEdit(row)"><el-icon><Edit /></el-icon>编辑</el-button>
            <el-button size="small" type="success" link @click="handleApprove(row)"><el-icon><Select /></el-icon>通过</el-button>
            <el-button size="small" type="danger" link @click="handleReject(row)"><el-icon><CloseBold /></el-icon>驳回</el-button>
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
          <el-form-item label="标题" prop="title">
            <el-input v-model="crud.dialog.form.title" placeholder="请输入标题" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="crud.dialog.form.description" placeholder="请输入描述" />
          </el-form-item>
          <el-form-item label="作业流ID" prop="workflowId">
            <el-input v-model="crud.dialog.form.workflowId" placeholder="请输入作业流ID" />
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
import { approvalApi } from '@/api/modules/approval'
import { useCRUD } from '@/composables/useCRUD'
import { ElMessage } from 'element-plus'
import SearchTable from '@/components/SearchTable.vue'
import { Edit, Delete, Select, CloseBold } from '@element-plus/icons-vue'

const defaultForm = () => ({
  title: '',
  description: '',
  workflowId: ''
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

const crud = useCRUD(approvalApi, defaultForm, rules, { entityName: '审批' })

async function handleApprove(row) {
  try {
    await approvalApi.approve(row.id)
    ElMessage.success('审批已通过')
    crud.table.fetch()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

async function handleReject(row) {
  try {
    await approvalApi.reject(row.id)
    ElMessage.success('审批已驳回')
    crud.table.fetch()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

onMounted(() => { crud.table.fetch() })
</script>