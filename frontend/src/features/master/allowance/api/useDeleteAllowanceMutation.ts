import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/master/allowance/api/queryKeys'

export const useDeleteAllowanceMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (id: number) =>
      del<void>('/api/master/allowances/{id}', {
        params: {
          path: { id },
        },
      }),

    onSuccess: async (_data: unknown, id: number) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.allowances.all }),
        queryClient.removeQueries({ queryKey: queryKeys.allowances.detail(id) }),
      ])
    },
  })
}