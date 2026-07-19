import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/batch/api/queryKeys'
import type {
  BatchJobDefinitionResponse,
  BatchJobDefinitionSaveRequest,
} from '@/features/system/batch/types/batchApiTypes'

export const useCreateBatchJobDefinitionMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: BatchJobDefinitionSaveRequest) =>
      await post<BatchJobDefinitionResponse, BatchJobDefinitionSaveRequest>(
        '/api/system/batch-jobs',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.batch.definitions.all,
      })
    },
  })
}