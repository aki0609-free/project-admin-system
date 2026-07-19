import { computed, reactive, watch, type Ref } from 'vue'
import { z } from 'zod'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type { EmployeeListItemResponse } from '../types/employeeApiTypes'
import type { EmployeeLoanForm } from '../types/employeeLoanSavingFormTypes'
import type { EmployeeLoanResponse } from '../types/employeeWorkApiTypes'
import {
  createEmptyEmployeeLoanForm,
  toEmployeeLoanForm,
} from '../utils/employeeLoanSavingFormFactory'

export const employeeLoanSchema = z.object({
  id: z.number(),
  employeeId: z.number().nullable(),
  principal: z.number().min(0),
  currentBalance: z.number().min(0),
  monthlyRepayment: z.number().min(0),
  loanDate: z.string(),
  repaymentStartDate: z.string(),
  activeFlag: z.boolean(),
  approvalStatus: z.enum(['PENDING', 'APPROVED', 'REJECTED']),
  approvalComment: z.string(),
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
      gridColumn: '1 / span 2',
    },
    {
      key: 'principal',
      label: '借入元本',
      type: 'number',
      gridColumn: '3 / span 1',
    },
    {
      key: 'currentBalance',
      label: '借入残高',
      type: 'number',
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
    {
      key: 'approvalStatus',
      label: '承認状態',
      type: 'select',
      gridColumn: '1 / span 2',
      options: [
        { title: '未承認', value: 'PENDING' },
        { title: '承認済', value: 'APPROVED' },
        { title: '却下', value: 'REJECTED' },
      ],
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

  const footerItems = computed<ToolbarItem[]>(() => {
    const items: ToolbarItem[] = []

    if (formModel.id > 0) {
      items.push({
        type: 'button',
        label: '削除',
        color: 'error',
        onClick: remove,
      })
    }

    items.push(
      {
        type: 'button',
        label: '閉じる',
        color: 'secondary',
        onClick: close,
      },
      {
        type: 'button',
        label: '保存',
        color: 'primary',
        onClick: save,
      },
    )

    return items
  })

  return {
    formModel,
    fields,
    schema: employeeLoanSchema,
    footerItems,
    isEdit: computed(() => formModel.id > 0),
  }
}