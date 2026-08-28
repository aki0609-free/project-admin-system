import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/application/api/queryKeys'

export const useDeleteApplicationMediaMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (id: number) =>
      del<unknown>('/api/application-media/{id}', {
        params: {
          path: { id },
        },
      }),

    onSuccess: async (_result: unknown, id: number) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.applicationMedias.all }),
        queryClient.removeQueries({ queryKey: queryKeys.applicationMedias.detail(id) }),
      ])
    },
  })
}
