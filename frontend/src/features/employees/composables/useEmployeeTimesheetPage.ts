import { computed } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useEmployeesQuery } from '../api/useEmployeesQuery'
import { useEmployeeTimesheetsQuery } from '../api/useEmployeeTimesheetsQuery'
import { useCreateEmployeeTimesheetMutation } from '../api/useCreateEmployeeTimesheetMutation'
import { useUpdateEmployeeTimesheetMutation } from '../api/useUpdateEmployeeTimesheetMutation'
import { useDeleteEmployeeTimesheetMutation } from '../api/useDeleteEmployeeTimesheetMutation'
import { EmployeeTimesheetForm } from '../types/employeeTimesheetFormTypes'
import { toEmployeeTimesheetSaveRequest } from '../utils/employeeTimesheetConverters'
import { useEmployeeTimesheetDialog } from './useEmployeeTimesheetDialog'

export const useEmployeeTimesheetPage = () => {
  const employeesQuery = useEmployeesQuery()
  const timesheetsQuery = useEmployeeTimesheetsQuery()
  const createMutation = useCreateEmployeeTimesheetMutation()
  const updateMutation = useUpdateEmployeeTimesheetMutation()
  const deleteMutation = useDeleteEmployeeTimesheetMutation()

  const dialog = useEmployeeTimesheetDialog()

  const busy = computed(
    () =>
      employeesQuery.isLoading.value ||
      timesheetsQuery.isLoading.value ||
      createMutation.isPending.value ||
      updateMutation.isPending.value ||
      deleteMutation.isPending.value,
  )

  const save = async (form: EmployeeTimesheetForm) => {
    const request = toEmployeeTimesheetSaveRequest(form)

    if (form.id > 0) {
      await updateMutation.mutateAsync({
        id: form.id,
        request,
      })
    } else {
      await createMutation.mutateAsync(request)
    }

    dialog.close()
  }

  const remove = async (form: EmployeeTimesheetForm) => {
    if (form.id <= 0) {
      dialog.close()
      return
    }

    const ok = window.confirm('この勤怠データを削除しますか？')
    if (!ok) return

    await deleteMutation.mutateAsync(form.id)
    dialog.close()
  }

  const toolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '新規作成',
      color: 'primary',
      disabled: busy.value,
      onClick: dialog.openCreate,
    },
  ])

  return {
    employeesQuery,
    timesheetsQuery,
    dialog,
    toolbarItems,
    save,
    remove,
  }
}