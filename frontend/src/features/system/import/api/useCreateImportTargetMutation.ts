import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/import/api/queryKeys'
import type {
  ImportTargetResponse,
  ImportTargetSaveRequest,
} from '@/features/system/import/types/importApiTypes'

export const useCreateImportTargetMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (request: ImportTargetSaveRequest) =>
      post<ImportTargetResponse, ImportTargetSaveRequest>(
        '/api/system/import-targets',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.importTargets.all,
      })
    },
  })
}