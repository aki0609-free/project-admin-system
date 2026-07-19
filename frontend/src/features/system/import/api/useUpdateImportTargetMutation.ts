import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
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

  return useAppMutation({
    mutationFn: ({ id, request }: Payload) =>
      put<ImportTargetResponse, ImportTargetSaveRequest>(
        `/api/system/import-targets/${id}`,
        request,
      ),

    onSuccess: async (_data: any, variables: any) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.importTargets.all,
      })
      await queryClient.invalidateQueries({
        queryKey: queryKeys.importTargets.detail(variables.id),
      })
    },
  })
}