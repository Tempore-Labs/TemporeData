import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * Composable for dialog form state management.
 *
 * @param {Function} getDefaultForm - returns a fresh default form object
 * @param {Object} rules - Element Plus form validation rules
 * @param {Object} options
 * @param {Function} options.onCreate - async (form) => created entity
 * @param {Function} options.onUpdate - async (id, form) => updated entity
 * @param {string} options.entityName - entity name for success messages (default '数据')
 * @returns dialog state and methods
 */
export function useDialog(getDefaultForm, rules = {}, options = {}) {
  const {
    onCreate,
    onUpdate,
    entityName = '数据'
  } = options

  const visible = ref(false)
  const isEdit = ref(false)
  const editId = ref(null)
  const submitting = ref(false)
  const formRef = ref(null)
  const form = reactive(getDefaultForm())

  // Open for create
  function openAdd() {
    isEdit.value = false
    editId.value = null
    Object.assign(form, getDefaultForm())
    visible.value = true
  }

  // Open for edit
  function openEdit(row) {
    isEdit.value = true
    editId.value = row.id
    Object.assign(form, getDefaultForm())
    // Merge row data into form (only fields that exist in defaultForm)
    const defaults = getDefaultForm()
    for (const key of Object.keys(defaults)) {
      if (key in row) {
        form[key] = row[key]
      }
    }
    visible.value = true
  }

  // Close dialog
  function close() {
    visible.value = false
  }

  // Validate form
  async function validate() {
    if (!formRef.value) return true
    try {
      await formRef.value.validate()
      return true
    } catch {
      return false
    }
  }

  // Submit form
  async function submit() {
    const valid = await validate()
    if (!valid) return false

    submitting.value = true
    try {
      const payload = { ...form }
      if (isEdit.value && onUpdate) {
        await onUpdate(editId.value, payload)
        ElMessage.success(`${entityName}更新成功`)
      } else if (onCreate) {
        await onCreate(payload)
        ElMessage.success(`${entityName}创建成功`)
      }
      visible.value = false
      return true
    } catch (err) {
      ElMessage.error(err.message || '操作失败')
      return false
    } finally {
      submitting.value = false
    }
  }

  return {
    visible,
    isEdit,
    editId,
    submitting,
    formRef,
    form,
    rules,
    openAdd,
    openEdit,
    close,
    validate,
    submit
  }
}