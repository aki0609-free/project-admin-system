import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/users/api/queryKeys'

export const useDeleteUserMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (id: number) =>
      del<void>('/api/users/{id}', {
        params: {
          path: { id },
        },
      }),

    onSuccess: async (_: any, id: any) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.users.all,
        }),
        queryClient.removeQueries({
          queryKey: queryKeys.users.detail(String(id)),
        }),
      ])
    },
  })
}