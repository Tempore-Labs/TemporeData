<template>
  <SearchTable
    title="黑白名单"
    :loading="crud.table.loading"
    :total="crud.table.total"
    v-model:current-page="crud.table.currentPage"
    v-model:page-size="crud.table.pageSize"
    add-text="新增名单"
    @add="crud.dialog.openAdd"
  >
    <template #default>
      <el-table :data="crud.table.pagedData" v-loading="crud.table.loading" stripe style="width: 100%">
        <el-table-column prop="ipAddress" label="IP地址" width="160" />
        <el-table-column prop="listType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.listType === 'BLACK' ? 'danger' : 'success'" effect="light">{{ row.listType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="crud.dialog.openEdit(row)"><el-icon><Edit /></el-icon>编辑</el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              size="small"
              type="warning"
              link
              @click="handleDisable(row)"
            >
              <el-icon><VideoPause /></el-icon>禁用
            </el-button>
            <el-button
              v-else
              size="small"
              type="success"
              link
              @click="handleEnable(row)"
            >
              <el-icon><VideoPlay /></el-icon>启用
            </el-button>
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
          <el-form-item label="IP地址" prop="ipAddress">
            <el-input v-model="crud.dialog.form.ipAddress" placeholder="请输入IP地址" />
          </el-form-item>
          <el-form-item label="类型" prop="listType">
            <el-select v-model="crud.dialog.form.listType" placeholder="请选择类型" style="width: 100%">
              <el-option label="BLACK" value="BLACK" />
              <el-option label="WHITE" value="WHITE" />
            </el-select>
          </el-form-item>
          <el-form-item label="原因" prop="reason">
            <el-input v-model="crud.dialog.form.reason" placeholder="请输入原因" />
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
import { blacklistApi } from '@/api/modules/blacklist'
import { useCRUD } from '@/composables/useCRUD'
import { ElMessage } from 'element-plus'
import SearchTable from '@/components/SearchTable.vue'
import { Edit, Delete, VideoPlay, VideoPause } from '@element-plus/icons-vue'

const defaultForm = () => ({
  ipAddress: '',
  listType: '',
  reason: ''
})

const rules = {
  ipAddress: [{ required: true, message: '请输入IP地址', trigger: 'blur' }],
  listType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const crud = useCRUD(blacklistApi, defaultForm, rules, { entityName: '名单' })

async function handleEnable(row) {
  try {
    await blacklistApi.enable(row.id)
    ElMessage.success('已启用')
    await crud.table.fetch()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

async function handleDisable(row) {
  try {
    await blacklistApi.disable(row.id)
    ElMessage.success('已禁用')
    await crud.table.fetch()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

onMounted(() => { crud.table.fetch() })
</script>