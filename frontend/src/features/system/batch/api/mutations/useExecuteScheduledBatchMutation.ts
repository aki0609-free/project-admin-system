import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/batch/api/queryKeys'
import type { BatchExecuteResponse } from '@/features/system/batch/types/batchApiTypes'

export const useExecuteScheduledBatchMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (jobCode: string) =>
      await post<BatchExecuteResponse, undefined>(
        `/api/system/batch/scheduled-execute/${encodeURIComponent(jobCode)}`,
        undefined,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.batch.logs.all })
      await queryClient.invalidateQueries({ queryKey: queryKeys.batch.definitions.all })
    },
  })
}