import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/master/deduction/api/queryKeys'

export const useDeleteDeductionGenericSettingMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (id: number) =>
      del<void>('/api/master/deductions/generic-settings/{id}', {
        params: {
          path: {
            id,
          },
        },
      }),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.deductions.all,
      })
    },
  })
}