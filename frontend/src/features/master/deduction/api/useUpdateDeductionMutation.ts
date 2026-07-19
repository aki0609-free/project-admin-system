import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/master/deduction/api/queryKeys'
import type { DeductionSaveRequest } from '@/features/master/deduction/types/deductionApiTypes'

export type UpdateDeductionPayload = {
  id: number
  body: DeductionSaveRequest
}

export const useUpdateDeductionMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({ id, body }: UpdateDeductionPayload) =>
      put<void, DeductionSaveRequest>('/api/master/deductions/{id}', body, {
        params: {
          path: { id },
        },
      }),

    onSuccess: async (_data: unknown, payload: UpdateDeductionPayload) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.deductions.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.deductions.detail(payload.id) }),
      ])
    },
  })
}