import { computed, ref } from 'vue'
import { useImportTargetsQuery } from '@/features/system/import/api/useImportTargetsQuery'
import { useExecuteImportMutation } from '@/features/system/import/api/useExecuteImportMutation'
import type { ImportSourceType } from '@/features/system/import/types/importApiTypes'

export type ImportExecutePayload = {
  targetCode: string
  sourceType: ImportSourceType
  file?: File | null
}

export const useImportExecuteTab = () => {
  const targetsQuery = useImportTargetsQuery(true)
  const executeMutation = useExecuteImportMutation()

  const lastResultMessage = ref('')
  const lastJobExecutionId = ref('')

  const pending = computed(() => executeMutation.isPending.value)

  const executeImport = async (payload: ImportExecutePayload) => {
    lastResultMessage.value = ''
    lastJobExecutionId.value = ''

    try {
      const result = await executeMutation.mutateAsync(payload) as any

      lastResultMessage.value = result.message ?? ''
      lastJobExecutionId.value = String(result.jobExecutionId ?? '')

      alert('インポートを開始しました。')
    } catch (error) {
      console.error(error)
      alert('インポートに失敗しました。')
    }
  }

  return {
    targetsQuery,
    pending,
    lastResultMessage,
    lastJobExecutionId,
    executeImport,
  }
}