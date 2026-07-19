import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/master/allowance/api/queryKeys'
import type { AllowanceSaveRequest } from '@/features/master/allowance/types/allowanceApiTypes'

export const useCreateAllowanceMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: AllowanceSaveRequest) =>
      post<number, AllowanceSaveRequest>('/api/master/allowances', payload),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.allowances.all,
      })
    },
  })
}