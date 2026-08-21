import { computed, reactive, watch, type Ref } from 'vue'
import { z } from 'zod'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import type { EmployeeListItemResponse } from '../types/employeeApiTypes'
import type { EmployeeSavingForm } from '../types/employeeLoanSavingFormTypes'
import type { EmployeeSavingResponse } from '../types/employeeWorkApiTypes'
import {
  createEmptyEmployeeSavingForm,
  toEmployeeSavingForm,
} from '../utils/employeeLoanSavingFormFactory'

export const employeeSavingSchema = z.object({
  id: z.number(),
  employeeId: z.number().nullable(),
  percentage: z.number(),
  minSalaryThreshold: z.number(),
  currentBalance: z.number().min(0),
  activeFlag: z.boolean(),
  approvalStatus: z.enum(['PENDING', 'APPROVED', 'REJECTED']),
  approvalComment: z.string(),
})

export const useEmployeeSavingEditDialog = (
  visible: Ref<boolean>,
  saving: Ref<EmployeeSavingResponse | null>,
  employees: Ref<EmployeeListItemResponse[]>,
  emitSave: (form: EmployeeSavingForm) => void,
  emitDelete: (form: EmployeeSavingForm) => void,
) => {
  const formModel = reactive<EmployeeSavingForm>(createEmptyEmployeeSavingForm())

  const resetForm = () => {
    Object.assign(formModel, createEmptyEmployeeSavingForm())
  }

  watch(
    () => visible.value,
    (opened) => {
      if (!opened) return
      if (!saving.value) resetForm()
    },
  )

  watch(
    () => saving.value,
    (value) => {
      if (!visible.value) return

      if (!value) {
        resetForm()
        return
      }

      Object.assign(formModel, toEmployeeSavingForm(value))
    },
    { immediate: true },
  )

  const employeeOptions = computed(() =>
    employees.value.map((employee) => ({
      title: `${employee.employeeCode} / ${employee.employeeName}`,
      value: employee.id,
    })),
  )

  const fields = computed(() => {
    const defs: GridFormFieldDef<EmployeeSavingForm>[] = [
      { key: 'id', label: 'ID', type: 'number', width: 90 },
      {
        key: 'employeeId',
        label: '従業員',
        type: 'select',
        options: employeeOptions.value,
        editable: formModel.id === 0,
        gridColumn: '2 / span 3',
      },
      { key: 'percentage', label: '貯蓄率%', type: 'number' },
      { key: 'minSalaryThreshold', label: '最低給与額', type: 'number' },
      {
        key: 'currentBalance',
        label: '積立残高',
        type: 'number',
        editable: false,
      },
      { key: 'activeFlag', label: '有効', type: 'checkbox' },
      {
        key: 'approvalComment',
        label: '承認コメント',
        type: 'textarea',
        rows: 4,
        autoGrow: true,
        gridColumn: '1 / -1',
      },
    ]

    return defs
  })

  const close = () => {
    visible.value = false
  }

  const save = () => {
    emitSave({ ...formModel })
  }

  const remove = () => {
    emitDelete({ ...formModel })
  }

  const leftFooterItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '削除',
      intent: 'danger',
      onClick: remove,
      visible: formModel.id > 0,
    },
  ])

  const rightFooterItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '閉じる',
      intent: 'secondary',
      onClick: close,
    },
    {
      type: 'button',
      label: '保存',
      intent: 'primary',
      onClick: save,
    },
  ])

  return {
    formModel,
    fields,
    schema: employeeSavingSchema,
    leftFooterItems,
    rightFooterItems,
    isEdit: computed(() => formModel.id > 0),
  }
}
