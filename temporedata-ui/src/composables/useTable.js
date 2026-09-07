import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * Composable for table state management: data, pagination, loading, search.
 *
 * @param {Function} apiFetch - async function that returns data array
 * @param {Object} options
 * @param {number} options.pageSize - default page size (default 10)
 * @returns table state and methods
 */
export function useTable(apiFetch, options = {}) {
  const { pageSize: defaultPageSize = 10 } = options

  const data = ref([])
  const loading = ref(false)
  const currentPage = ref(1)
  const pageSize = ref(defaultPageSize)
  const searchKeyword = ref('')
  const error = ref(null)

  // Paginated data
  const pagedData = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    return data.value.slice(start, start + pageSize.value)
  })

  // Total count
  const total = computed(() => data.value.length)

  // Fetch data
  async function fetch() {
    loading.value = true
    error.value = null
    try {
      const result = await apiFetch()
      data.value = Array.isArray(result) ? result : []
    } catch (err) {
      error.value = err
      ElMessage.error(err.message || '加载数据失败')
    } finally {
      loading.value = false
    }
  }

  // Reset pagination
  function resetPage() {
    currentPage.value = 1
  }

  // Handle page size change
  function onPageSizeChange(size) {
    pageSize.value = size
    currentPage.value = 1
  }

  return {
    data,
    loading,
    error,
    currentPage,
    pageSize,
    pagedData,
    total,
    searchKeyword,
    fetch,
    resetPage,
    onPageSizeChange
  }
}