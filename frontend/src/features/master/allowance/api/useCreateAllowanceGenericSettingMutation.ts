import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/master/allowance/api/queryKeys'
import type { AllowanceGenericSettingSaveRequest } from '@/features/master/allowance/types/allowanceApiTypes'

export const useCreateAllowanceGenericSettingMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: AllowanceGenericSettingSaveRequest) =>
      post<number, AllowanceGenericSettingSaveRequest>(
        '/api/master/allowances/generic-settings',
        payload,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.allowances.all,
      })
    },
  })
}