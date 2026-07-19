import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/system/import/api/queryKeys'

type Payload = {
  id: number
}

export const useDeleteImportTargetMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({ id }: Payload) =>
      del<void>(`/api/system/import-targets/${id}`),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.importTargets.all,
      })
    },
  })
}