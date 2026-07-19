import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/batch/api/queryKeys'
import type {
  BatchExecutePayload,
  BatchExecuteRequest,
  BatchExecuteResponse,
} from '@/features/system/batch/types/batchApiTypes'

export const useExecuteBatchMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ jobCode, params }: BatchExecutePayload) =>
      await post<BatchExecuteResponse, BatchExecuteRequest>(
        `/api/system/batch/execute/${encodeURIComponent(jobCode)}`,
        {
          params: params ?? {},
        },
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.batch.logs.all })
      await queryClient.invalidateQueries({ queryKey: queryKeys.batch.definitions.all })
    },
  })
}