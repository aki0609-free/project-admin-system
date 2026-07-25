import {
  useMutation,
  useQueryClient,
} from '@tanstack/vue-query'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/system/import/api/queryKeys'
import type {
  ImportTargetResponse,
  ImportTargetSaveRequest,
} from '@/features/system/import/types/importApiTypes'

type Payload = {
  id: number
  request: ImportTargetSaveRequest
}

export const useUpdateImportTargetMutation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, request }: Payload) =>
      put<ImportTargetResponse, ImportTargetSaveRequest>(
        `/api/system/import-targets/${id}`,
        request,
      ),

    onSuccess: async (
      _data,
      variables,
    ) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.importTargets.all,
      })
      await queryClient.invalidateQueries({
        queryKey: queryKeys.importTargets.detail(variables.id),
      })
    },
  })
}
