import { computed, ref } from 'vue'
import { useEmployeeDetailQuery } from '../api/useEmployeeDetailQuery'
import type { EmployeeTableRow } from './useEmployeeTableConfig'

export const useEmployeeDialog = () => {
  const visible = ref(false)
  const editingId = ref<number | null>(null)

  const detailQuery = useEmployeeDetailQuery(
    computed(() => editingId.value),
  )

  const dialogEmployee = computed(() =>
    editingId.value == null ? null : detailQuery.employee.value,
  )

  const openCreate = () => {
    editingId.value = null
    visible.value = true
  }

  const openEdit = (row: EmployeeTableRow) => {
    editingId.value = row.id
    visible.value = true
  }

  const close = () => {
    visible.value = false
  }

  return {
    visible,
    editingId,
    detailQuery,
    dialogEmployee,
    openCreate,
    openEdit,
    close,
  }
}