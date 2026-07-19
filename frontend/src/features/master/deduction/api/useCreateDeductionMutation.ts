import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/master/deduction/api/queryKeys'
import type { DeductionSaveRequest } from '@/features/master/deduction/types/deductionApiTypes'

export const useCreateDeductionMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: DeductionSaveRequest) =>
      post<number, DeductionSaveRequest>('/api/master/deductions', payload),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.deductions.all,
      })
    },
  })
}