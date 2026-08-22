import { computed, reactive, watch, type Ref } from 'vue'
import { z } from 'zod'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import type { EmployeeListItemResponse } from '../types/employeeApiTypes'
import type { EmployeeLoanForm } from '../types/employeeLoanSavingFormTypes'
import type { EmployeeLoanResponse } from '../types/employeeWorkApiTypes'
import {
  createEmptyEmployeeLoanForm,
  toEmployeeLoanForm,
} from '../utils/employeeLoanSavingFormFactory'

export const employeeLoanSchema = z.object({
  id: z.number(),
  employeeId: z
    .number()
    .nullable()
    .refine((value): boolean => value != null, '従業員は必須です。'),
  principal: z.number().positive('借入元本は0円より大きい金額を指定してください。'),
  currentBalance: z.number().min(0),
  monthlyRepayment: z.number().min(0, '月返済額は0円以上で指定してください。'),
  loanDate: z.string(),
  repaymentStartDate: z.string(),
  activeFlag: z.boolean(),
})

export const useEmployeeLoanEditDialog = (
  visible: Ref<boolean>,
  loan: Ref<EmployeeLoanResponse | null>,
  employees: Ref<EmployeeListItemResponse[]>,
  emitSave: (form: EmployeeLoanForm) => void,
  emitDelete: (form: EmployeeLoanForm) => void,
) => {
  const formModel = reactive<EmployeeLoanForm>(createEmptyEmployeeLoanForm())

  const resetForm = () => {
    Object.assign(formModel, createEmptyEmployeeLoanForm())
  }

  watch(
    () => visible.value,
    (opened) => {
      if (!opened) return
      if (!loan.value) resetForm()
    },
  )

  watch(
    () => loan.value,
    (value) => {
      if (!visible.value) return

      if (!value) {
        resetForm()
        return
      }

      Object.assign(formModel, toEmployeeLoanForm(value))
    },
    { immediate: true },
  )

  const employeeOptions = computed(() =>
    employees.value.map((employee) => ({
      title: `${employee.employeeCode} / ${employee.employeeName}`,
      value: employee.id,
    })),
  )

  const fields = computed<GridFormFieldDef<EmployeeLoanForm>[]>(() => [
    {
      key: 'employeeId',
      label: '従業員',
      type: 'select',
      options: employeeOptions.value,
      editable: formModel.id === 0,
      gridColumn: '1 / span 2',
    },
    {
      key: 'principal',
      label: '借入元本',
      type: 'number',
      editable:
        formModel.id === 0 ||
        formModel.currentBalance === (loan.value?.principal ?? formModel.principal),
      gridColumn: '3 / span 1',
    },
    {
      key: 'currentBalance',
      label: '借入残高',
      type: 'number',
      editable: false,
      gridColumn: '4 / span 1',
    },
    {
      key: 'monthlyRepayment',
      label: '月返済額',
      type: 'number',
      gridColumn: '1 / span 1',
    },
    {
      key: 'loanDate',
      label: '借入日',
      type: 'date',
      gridColumn: '2 / span 1',
    },
    {
      key: 'repaymentStartDate',
      label: '返済開始日',
      type: 'date',
      gridColumn: '3 / span 1',
    },
    {
      key: 'activeFlag',
      label: '有効',
      type: 'checkbox',
      gridColumn: '4 / span 1',
      width: 120,
    },
  ])

  const close = () => {
    visible.value = false
  }

  const save = () => {
    emitSave({ ...formModel })
  }

  const remove = () => {
    emitDelete({ ...formModel })
  }

  const leftFooterItems = computed<ToolbarItem[]>(() =>
    formModel.id > 0
      ? [
          {
            type: 'button',
            label: '削除',
            intent: 'danger',
            onClick: remove,
          },
        ]
      : [],
  )

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
    schema: employeeLoanSchema,
    leftFooterItems,
    rightFooterItems,
    isEdit: computed(() => formModel.id > 0),
  }
}
