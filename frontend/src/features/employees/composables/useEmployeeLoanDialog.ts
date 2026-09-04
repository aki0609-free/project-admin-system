import { ref } from 'vue'
import type { EmployeeLoanResponse } from '../types/employeeWorkApiTypes'
import type { EmployeeLoanTableRow } from './useEmployeeLoanTableConfig'

export const useEmployeeLoanDialog = () => {
  const visible = ref(false)
  const selected = ref<EmployeeLoanResponse | null>(null)

  const openCreate = () => {
    selected.value = null
    visible.value = true
  }

  const openEdit = (row: EmployeeLoanTableRow) => {
    selected.value = row.raw
    visible.value = true
  }

  const close = () => {
    visible.value = false
  }

  return { visible, selected, openCreate, openEdit, close }
}
