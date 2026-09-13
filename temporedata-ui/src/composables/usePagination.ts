/**
 * Pagination helper for el-pagination driven lists.
 */
import { ref, computed } from 'vue'

export function usePagination(initialSize = 10) {
  const page = ref(1)
  const size = ref(initialSize)
  const total = ref(0)

  const pagination = computed(() => ({
    currentPage: page.value,
    pageSize: size.value,
    total: total.value,
  }))

  function onPageChange(p: number) {
    page.value = p
  }

  function onSizeChange(s: number) {
    size.value = s
    page.value = 1
  }

  function reset() {
    page.value = 1
  }

  return { page, size, total, pagination, onPageChange, onSizeChange, reset }
}
