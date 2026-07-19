import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/master/allowance/api/queryKeys'

export const useDeleteAllowanceGenericSettingMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (id: number) =>
      del<void>('/api/master/allowances/generic-settings/{id}', {
        params: {
          path: {
            id,
          },
        },
      }),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.allowances.all,
      })
    },
  })
}