/* eslint-disable @typescript-eslint/no-invalid-void-type */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import type { CustomerSaveRequest } from '../types/customerApiTypes'
import { queryKeys } from './queryKeys'

export type UpdateCustomerPayload = {
  id: number
  body: CustomerSaveRequest
}

export const useUpdateCustomerMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({ id, body }: UpdateCustomerPayload) =>
      put<void, CustomerSaveRequest>('/api/customers/{id}', body, {
        params: {
          path: { id },
        },
      }),

    onSuccess: async (_data: unknown, payload: UpdateCustomerPayload) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.customers.all,
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.customers.detail(payload.id),
        }),
      ])
    },
  })
}