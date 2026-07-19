import { reactive, watch, type Ref } from 'vue'
import type { BatchJobDefinitionResponse } from '@/features/system/batch/types/batchApiTypes'
import type { BatchJobDefinitionForm } from '@/features/system/batch/types/batchFormTypes'
import {
  createEmptyBatchJobDefinitionForm,
  toBatchJobDefinitionForm,
} from '@/features/system/batch/utils/batchJobDefinitionFormFactory'

export const useBatchDefinitionDialogState = (
  visible: Ref<boolean>,
  definition: Ref<BatchJobDefinitionResponse | null>,
) => {
  const formModel = reactive<BatchJobDefinitionForm>(
    createEmptyBatchJobDefinitionForm(),
  )

  const resetForm = () => {
    Object.assign(formModel, createEmptyBatchJobDefinitionForm())
  }

  const applyDefinition = (value: BatchJobDefinitionResponse) => {
    Object.assign(formModel, toBatchJobDefinitionForm(value))
  }

  watch(
    () => visible.value,
    opened => {
      if (!opened) return

      if (!definition.value) {
        resetForm()
        return
      }

      applyDefinition(definition.value)
    },
    { immediate: true },
  )

  watch(
    () => definition.value,
    value => {
      if (!visible.value) return

      if (!value) {
        resetForm()
        return
      }

      applyDefinition(value)
    },
    { immediate: true },
  )

  return {
    formModel,
    resetForm,
    applyDefinition,
  }
}