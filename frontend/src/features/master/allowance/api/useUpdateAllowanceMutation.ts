import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/master/allowance/api/queryKeys'
import type { AllowanceSaveRequest } from '@/features/master/allowance/types/allowanceApiTypes'

export type UpdateAllowancePayload = {
  id: number
  body: AllowanceSaveRequest
}

export const useUpdateAllowanceMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({ id, body }: UpdateAllowancePayload) =>
      put<void, AllowanceSaveRequest>('/api/master/allowances/{id}', body, {
        params: {
          path: { id },
        },
      }),

    onSuccess: async (_data: unknown, payload: UpdateAllowancePayload) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.allowances.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.allowances.detail(payload.id) }),
      ])
    },
  })
}