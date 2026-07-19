import { ref } from 'vue'
import { EmployeeTimesheetResponse } from '../types/employeeWorkApiTypes'
import { EmployeeTimesheetTableRow } from './useEmployeeTimesheetTableConfig'

export const useEmployeeTimesheetDialog = () => {
  const visible = ref(false)
  const selected = ref<EmployeeTimesheetResponse | null>(null)

  const openCreate = () => {
    selected.value = null
    visible.value = true
  }

  const openEdit = (row: EmployeeTimesheetTableRow) => {
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