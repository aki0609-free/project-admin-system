import {
  useMutation,
  useQueryClient,
} from '@tanstack/vue-query'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/system/import/api/queryKeys'

type Payload = {
  id: number
}

export const useDeleteImportTargetMutation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id }: Payload) =>
      del<unknown>(`/api/system/import-targets/${id}`),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.importTargets.all,
      })
    },
  })
}
