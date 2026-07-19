import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/master/deduction/api/queryKeys'
import type { DeductionGenericSettingSaveRequest } from '@/features/master/deduction/types/deductionApiTypes'

export const useCreateDeductionGenericSettingMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: DeductionGenericSettingSaveRequest) =>
      post<number, DeductionGenericSettingSaveRequest>(
        '/api/master/deductions/generic-settings',
        payload,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.deductions.all,
      })
    },
  })
}