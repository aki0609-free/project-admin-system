import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/master/allowance/api/queryKeys'
import type { AllowanceGenericSettingSaveRequest } from '@/features/master/allowance/types/allowanceApiTypes'

export type UpdateAllowanceGenericSettingPayload = {
  id: number
  body: AllowanceGenericSettingSaveRequest
}

export const useUpdateAllowanceGenericSettingMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: UpdateAllowanceGenericSettingPayload) =>
      put<void, AllowanceGenericSettingSaveRequest>(
        '/api/master/allowances/generic-settings/{id}',
        payload.body,
        {
          params: {
            path: {
              id: payload.id,
            },
          },
        },
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.allowances.all,
      })
    },
  })
}