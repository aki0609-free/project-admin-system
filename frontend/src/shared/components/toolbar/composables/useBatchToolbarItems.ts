import { computed, type ComputedRef } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useExecuteBatchMutation } from '@/features/system/batch/api/mutations/useExecuteBatchMutation'
import { type BatchExecuteResponse } from '@/features/system/batch/types/batchApiTypes'

export type BatchToolbarButtonConfig = {
  label: string
  jobCode: string
  color?: string
  disabled?: boolean | ComputedRef<boolean>
  confirmMessage?: string
  successMessage?: (message: string) => string
  params?: () => Record<string, unknown>
}

export const useBatchToolbarItems = () => {
  const executeBatchMutation = useExecuteBatchMutation()

  const isExecutingBatch = computed(() => executeBatchMutation.isPending.value)

  const resolveDisabled = (disabled?: boolean | ComputedRef<boolean>) => {
    if (typeof disabled === 'boolean') return disabled
    return disabled?.value ?? false
  }

  const createBatchButton = (config: BatchToolbarButtonConfig): ToolbarItem => ({
    type: 'button',
    label: config.label,
    color: config.color ?? 'secondary',
    disabled: isExecutingBatch.value || resolveDisabled(config.disabled),
    onClick: async () => {
      if (config.confirmMessage) {
        const ok = window.confirm(config.confirmMessage)
        if (!ok) return
      }

      const result = await executeBatchMutation.mutateAsync({
        jobCode: config.jobCode,
        params: config.params ? config.params() : {},
      }) as BatchExecuteResponse

      alert(
        config.successMessage
          ? config.successMessage(result.message)
          : result.message,
      )
    },
  })

  return {
    isExecutingBatch,
    createBatchButton,
  }
}