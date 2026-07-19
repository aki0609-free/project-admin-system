import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/master/deduction/api/queryKeys'

export const useDeleteDeductionMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (id: number) =>
      del<void>('/api/master/deductions/{id}', {
        params: {
          path: { id },
        },
      }),

    onSuccess: async (_data: unknown, id: number) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.deductions.all }),
        queryClient.removeQueries({ queryKey: queryKeys.deductions.detail(id) }),
      ])
    },
  })
}