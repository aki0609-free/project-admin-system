import { computed, reactive, watch, type Ref } from 'vue'
import { z } from 'zod'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { EmployeeListItemResponse } from '../types/employeeApiTypes'
import { EmployeeTimesheetForm } from '../types/employeeTimesheetFormTypes'
import { EmployeeTimesheetResponse } from '../types/employeeWorkApiTypes'
import { createEmptyEmployeeTimesheetForm, toEmployeeTimesheetForm } from '../utils/employeeTimesheetFormFactory'

export const employeeTimesheetSchema = z.object({
  id: z.number(),
  employeeId: z.number().nullable(),
  workDate: z.string().min(1, '必須です'),
  clockIn: z.string(),
  clockOut: z.string(),
  workHours: z.number().min(0),
  overtimeHours: z.number().min(0),
  nightShiftHours: z.number().min(0),
  weekendFlag: z.boolean(),
  approvalStatus: z.enum(['PENDING', 'APPROVED', 'REJECTED']),
  approvalComment: z.string(),
})

export const useEmployeeTimesheetEditDialog = (
  visible: Ref<boolean>,
  timesheet: Ref<EmployeeTimesheetResponse | null>,
  employees: Ref<EmployeeListItemResponse[]>,
  emitSave: (form: EmployeeTimesheetForm) => void,
  emitDelete: (form: EmployeeTimesheetForm) => void,
) => {
  const formModel = reactive<EmployeeTimesheetForm>(
    createEmptyEmployeeTimesheetForm(),
  )

  const resetForm = () => {
    Object.assign(formModel, createEmptyEmployeeTimesheetForm())
  }

  watch(
    () => visible.value,
    (opened) => {
      if (!opened) return
      if (!timesheet.value) resetForm()
    },
    { immediate: true },
  )

  watch(
    () => timesheet.value,
    (value) => {
      if (!visible.value) return

      if (!value) {
        resetForm()
        return
      }

      Object.assign(formModel, toEmployeeTimesheetForm(value))
    },
    { immediate: true },
  )

  const isEdit = computed(() => formModel.id > 0)

  const employeeOptions = computed(() =>
    employees.value.map((employee) => ({
      title: `${employee.employeeCode} / ${employee.employeeName}`,
      value: employee.id,
    })),
  )

  const fields = computed(() => {
    const defs: GridFormFieldDef<EmployeeTimesheetForm>[] = [
      { key: 'id', label: 'ID', type: 'number', width: 90 },
      {
        key: 'employeeId',
        label: '従業員',
        type: 'select',
        gridColumn: '2 / span 3',
        options: employeeOptions.value,
      },
      { key: 'workDate', label: '勤務日', type: 'date' },
      { key: 'clockIn', label: '出勤', type: 'text' },
      { key: 'clockOut', label: '退勤', type: 'text' },
      { key: 'workHours', label: '通常時間', type: 'number' },
      { key: 'overtimeHours', label: '残業時間', type: 'number' },
      { key: 'nightShiftHours', label: '深夜時間', type: 'number' },
      { key: 'weekendFlag', label: '土日出勤', type: 'checkbox', width: 120 },
      {
        key: 'approvalStatus',
        label: '承認状態',
        type: 'select',
        options: [
          { title: '未承認', value: 'PENDING' },
          { title: '承認済', value: 'APPROVED' },
          { title: '却下', value: 'REJECTED' },
        ],
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

  const footerItems = computed<ToolbarItem[]>(() => {
    const items: ToolbarItem[] = []

    if (isEdit.value) {
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
    isEdit,
    fields,
    schema: employeeTimesheetSchema,
    footerItems,
  }
}