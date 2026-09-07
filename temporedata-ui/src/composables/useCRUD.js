import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useTable } from './useTable'
import { useDialog } from './useDialog'

/**
 * Composable that combines useTable + useDialog for full CRUD operations.
 * Returns a reactive object so refs are auto-unwrapped in templates.
 * Usage: const crud = useCRUD(api, defaultForm, rules)
 *   Template: crud.table.loading, crud.dialog.visible, v-model="crud.dialog.form.name"
 *
 * @param {Object} api - API module with { list, create, update, delete } methods
 * @param {Function} getDefaultForm - returns a fresh default form object
 * @param {Object} rules - Element Plus form validation rules
 * @param {Object} options
 * @param {string} options.entityName - entity name for messages (default '数据')
 * @param {number} options.pageSize - default page size (default 10)
 * @returns reactive { table, dialog, handleDelete, handleSubmit }
 */
export function useCRUD(api, getDefaultForm, rules = {}, options = {}) {
  const { entityName = '数据', pageSize } = options

  const table = useTable(() => api.list(), { pageSize })
  const dialog = useDialog(getDefaultForm, rules, {
    entityName,
    onCreate: (payload) => api.create(payload),
    onUpdate: (id, payload) => api.update(id, payload)
  })

  async function handleDelete(id) {
    try {
      await api.delete(id)
      ElMessage.success(`${entityName}已删除`)
      if (table.pagedData.value.length === 1 && table.currentPage.value > 1) {
        table.currentPage.value--
      }
      await table.fetch()
    } catch (err) {
      ElMessage.error(err.message || '删除失败')
    }
  }

  async function handleSubmit() {
    const success = await dialog.submit()
    if (success) {
      await table.fetch()
    }
  }

  return reactive({
    table,
    dialog,
    handleDelete,
    handleSubmit
  })
}