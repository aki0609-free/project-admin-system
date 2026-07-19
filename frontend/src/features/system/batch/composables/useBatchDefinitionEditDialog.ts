import { computed, type Ref } from 'vue'
import type { BatchJobDefinitionResponse } from '@/features/system/batch/types/batchApiTypes'
import type { BatchJobDefinitionForm } from '@/features/system/batch/types/batchFormTypes'
import { batchDefinitionSchema } from './useBatchDefinitionSchema'
import { useBatchDefinitionDialogState } from './useBatchDefinitionDialogState'
import { useBatchDefinitionFields } from './useBatchDefinitionFields'
import { useBatchDefinitionFooterItems } from './useBatchDefinitionFooterItems'

export const useBatchDefinitionEditDialog = (
  visible: Ref<boolean>,
  definition: Ref<BatchJobDefinitionResponse | null>,
  emitSave: (form: BatchJobDefinitionForm) => void,
  emitDelete: (form: BatchJobDefinitionForm) => void,
  emitExecuteNow: (form: BatchJobDefinitionForm) => void,
  emitExecuteScheduled: (form: BatchJobDefinitionForm) => void,
) => {
  const { formModel } = useBatchDefinitionDialogState(
    visible,
    definition,
  )

  const isEdit = computed(() => formModel.id > 0)

  const title = computed(() =>
    isEdit.value
      ? 'Batch定義編集'
      : 'Batch定義新規作成',
  )

  const { fields } = useBatchDefinitionFields(formModel)

  const {
    leftFooterItems,
    rightFooterItems,
  } = useBatchDefinitionFooterItems(
    visible,
    isEdit,
    formModel,
    emitSave,
    emitDelete,
    emitExecuteNow,
    emitExecuteScheduled,
  )

  return {
    formModel,
    isEdit,
    title,
    fields,
    schema: batchDefinitionSchema,
    leftFooterItems,
    rightFooterItems,
  }
}