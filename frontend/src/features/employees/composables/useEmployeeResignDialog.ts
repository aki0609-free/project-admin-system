import { computed, reactive, watch, type Ref } from 'vue'
import { z } from 'zod'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import type { EmployeeResignRequest } from '../types/employeeApiTypes'
import type { EmployeeForm } from '../types/employeeFormTypes'

export type EmployeeResignForm = {
  resignDate: string
  checkedChecklistIds: number[]
  note: string
}

export const employeeResignSchema = z.object({
  resignDate: z.string().min(1, '退職日は必須です。'),
  checkedChecklistIds: z.array(z.number()),
  note: z.string(),
})

export const useEmployeeResignDialog = (
  visible: Ref<boolean>,
  employee: Ref<EmployeeForm>,
  requiredChecklistIds: Ref<number[]>,
  emitSubmit: (request: EmployeeResignRequest) => void,
) => {
  const formModel = reactive<EmployeeResignForm>({
    resignDate: '',
    checkedChecklistIds: [],
    note: '',
  })

  const resetForm = () => {
    formModel.resignDate = employee.value.resignDate || ''
    formModel.checkedChecklistIds = []
    formModel.note = ''
  }

  watch(
    () => visible.value,
    (opened) => {
      if (!opened) return
      resetForm()
    },
  )

  const missingRequiredIds = computed(() =>
    requiredChecklistIds.value.filter((id) => !formModel.checkedChecklistIds.includes(id)),
  )

  const canSubmit = computed(
    () => formModel.resignDate.trim().length > 0 && missingRequiredIds.value.length === 0,
  )

  const dateFields: GridFormFieldDef<EmployeeResignForm>[] = [
    {
      key: 'resignDate',
      label: '退職日',
      type: 'date',
      gridColumn: '1 / span 2',
    },
  ]

  const noteFields: GridFormFieldDef<EmployeeResignForm>[] = [
    {
      key: 'note',
      label: '備考',
      type: 'textarea',
      rows: 3,
      autoGrow: true,
      gridColumn: '1 / -1',
    },
  ]

  const close = () => {
    visible.value = false
  }

  const submit = () => {
    if (!canSubmit.value) return

    emitSubmit({
      resignDate: formModel.resignDate,
      checkedChecklistIds: [...formModel.checkedChecklistIds],
      note: formModel.note.trim() ? formModel.note : null,
    })
  }

  const rightFooterItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '閉じる',
      intent: 'secondary',
      onClick: close,
    },
    {
      type: 'button',
      label: '退職処理を実行',
      intent: 'warning',
      disabled: !canSubmit.value,
      onClick: submit,
    },
  ])

  return {
    formModel,
    missingRequiredIds,
    canSubmit,
    dateFields,
    noteFields,
    rightFooterItems,
    schema: employeeResignSchema,
  }
}
