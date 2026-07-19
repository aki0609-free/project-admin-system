import { ref } from 'vue'
import { EmployeeSavingResponse } from '../types/employeeWorkApiTypes'
import { EmployeeSavingTableRow } from './useEmployeeSavingTableConfig'

export const useEmployeeSavingDialog = () => {
  const visible = ref(false)
  const selected = ref<EmployeeSavingResponse | null>(null)

  const openCreate = () => {
    selected.value = null
    visible.value = true
  }

  const openEdit = (row: EmployeeSavingTableRow) => {
    selected.value = row.raw
    visible.value = true
  }

  const close = () => {
    visible.value = false
  }

  return {
    visible,
    selected,
    openCreate,
    openEdit,
    close,
  }
}