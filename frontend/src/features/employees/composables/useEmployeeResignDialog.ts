import { computed, reactive, watch, type Ref } from 'vue'
import { z } from 'zod'
import type { EmployeeResignRequest } from '../types/employeeApiTypes'
import type { EmployeeForm } from '../types/employeeFormTypes'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'

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
    requiredChecklistIds.value.filter(
      (id) => !formModel.checkedChecklistIds.includes(id),
    ),
  )

  const canSubmit = computed(
    () =>
      formModel.resignDate.trim().length > 0 &&
      missingRequiredIds.value.length === 0,
  )

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

  const footerItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '閉じる',
      color: 'secondary',
      onClick: close,
    },
    {
      type: 'button',
      label: '退職処理を実行',
      color: 'warning',
      disabled: !canSubmit.value,
      onClick: submit,
    },
  ])

  return {
    formModel,
    missingRequiredIds,
    canSubmit,
    footerItems,
    schema: employeeResignSchema,
  }
}