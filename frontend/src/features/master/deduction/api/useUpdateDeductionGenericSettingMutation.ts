import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/master/deduction/api/queryKeys'
import type { DeductionGenericSettingSaveRequest } from '@/features/master/deduction/types/deductionApiTypes'

export type UpdateDeductionGenericSettingPayload = {
  id: number
  body: DeductionGenericSettingSaveRequest
}

export const useUpdateDeductionGenericSettingMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: UpdateDeductionGenericSettingPayload) =>
      put<void, DeductionGenericSettingSaveRequest>(
        '/api/master/deductions/generic-settings/{id}',
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
        queryKey: queryKeys.deductions.all,
      })
    },
  })
}