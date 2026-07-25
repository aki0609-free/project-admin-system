import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/batch/api/queryKeys'
import type { BatchExecuteResponse } from '@/features/system/batch/types/batchApiTypes'

export const useRetryBatchMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (logId: number) =>
      await post<BatchExecuteResponse, undefined>(
        `/api/system/batch/retry/${logId}`,
        undefined,
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.batch.logs.all })
      await queryClient.invalidateQueries({ queryKey: queryKeys.batch.definitions.all })
    },
  })
}
