import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/system/batch/api/queryKeys'
import type {
  BatchJobDefinitionResponse,
  BatchJobDefinitionSaveRequest,
} from '@/features/system/batch/types/batchApiTypes'

type Payload = {
  id: number
  request: BatchJobDefinitionSaveRequest
}

export const useUpdateBatchJobDefinitionMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id, request }: Payload) =>
      await put<BatchJobDefinitionResponse, BatchJobDefinitionSaveRequest>(
        `/api/system/batch-jobs/${id}`,
        request,
      ),

    onSuccess: async (_data: unknown, variables: Payload) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.batch.definitions.all,
      })
      await queryClient.invalidateQueries({
        queryKey: queryKeys.batch.definitions.detail(variables.id),
      })
    },
  })
}