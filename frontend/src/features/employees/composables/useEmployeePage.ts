import { computed } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useEmployeesQuery } from '../api/useEmployeesQuery'
import { useCreateEmployeeMutation } from '../api/useCreateEmployeeMutation'
import { useUpdateEmployeeMutation } from '../api/useUpdateEmployeeMutation'
import { useDeleteEmployeeMutation } from '../api/useDeleteEmployeeMutation'
import { useEmployeeDialog } from '../composables/useEmployeeDialog'
import { toEmployeeSaveRequest } from '../utils/employeeConverters'
import type { EmployeeForm } from '../types/employeeFormTypes'
import { useBatchToolbarItems } from '@/shared/components/toolbar/composables/useBatchToolbarItems'
import { batchParams, createBatchItem, createButtonItem } from '@/toolbox/toolbar/utils/toolbarItemFactory'

export const useEmployeePage = () => {
  const employeesQuery = useEmployeesQuery()
  const createMutation = useCreateEmployeeMutation()
  const updateMutation = useUpdateEmployeeMutation()
  const deleteMutation = useDeleteEmployeeMutation()

  const dialog = useEmployeeDialog()
  const { createBatchButton, isExecutingBatch } = useBatchToolbarItems()

  const busy = computed(
    () =>
      employeesQuery.isLoading.value ||
      dialog.detailQuery.isLoading.value ||
      createMutation.isPending.value ||
      updateMutation.isPending.value ||
      deleteMutation.isPending.value ||
      isExecutingBatch.value,
  )

  const save = async (form: EmployeeForm) => {
    const request = toEmployeeSaveRequest(form)

    if (form.id > 0) {
      await updateMutation.mutateAsync({ id: form.id, request })
    } else {
      await createMutation.mutateAsync(request)
    }

    dialog.close()
  }

  const remove = async (form: EmployeeForm) => {
    if (form.id <= 0) {
      dialog.close()
      return
    }

    const ok = window.confirm(`「${form.employeeName}」を削除しますか？`)
    if (!ok) return

    await deleteMutation.mutateAsync(form.id)
    dialog.close()
  }

  const leftToolbarItems = computed<ToolbarItem[]>(() => [
    createButtonItem({
      label: '新規作成',
      color: 'primary',
      disabled: busy.value,
      onClick: dialog.openCreate,
    }),
  ])

  const rightToolbarItems = computed<ToolbarItem[]>(() => [

    createBatchItem({
      label: '給与明細出力',
      jobCode: 'REPORT_EMPLOYEE_PAYROLL',
      disabled: busy.value,
      confirmMessage: '従業員の給与明細を出力しますか？',
      parameterDefinitions: [
        batchParams.checkbox({
          key: 'includeDeleted',
          label: '削除済みを含める',
          defaultValue: false,
        }),
      ],
    }),

    createBatchItem({
      label: '従業員CSV出力',
      jobCode: 'EXPORT_EMPLOYEE_CSV',
      disabled: busy.value,
      confirmMessage: '従業員CSVを出力しますか？',
      parameterDefinitions: [
        batchParams.checkbox({
          key: 'includeDeleted',
          label: '削除済みを含める',
          defaultValue: false,
        }),
      ],
    }),

    createBatchItem({
      label: '従業員データ取込',
      jobCode: 'IMPORT_EMPLOYEE',
      disabled: busy.value,
      confirmMessage: '従業員データを取り込みますか？',
      parameterDefinitions: [
        batchParams.text({
          key: 'targetCode',
          label: '取込定義コード',
          required: true,
          defaultValue: 'IMPORT_EMPLOYEE',
        }),
      ],
    }),
  ])

  return {
    employeesQuery,
    dialog,
    busy,
    leftToolbarItems,
    rightToolbarItems,
    save,
    remove,
  }
}
