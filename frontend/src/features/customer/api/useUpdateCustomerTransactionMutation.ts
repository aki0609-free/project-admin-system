/* eslint-disable @typescript-eslint/no-invalid-void-type */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import type { CustomerTransactionRequest } from '../types/customerApiTypes'
import { queryKeys } from './queryKeys'

export type UpdateCustomerTransactionPayload = {
  customerId: number
  transactionId: number
  body: CustomerTransactionRequest
}

export const useUpdateCustomerTransactionMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({ customerId, transactionId, body }: UpdateCustomerTransactionPayload) =>
      put<void, CustomerTransactionRequest>(
        '/api/customers/{customerId}/transactions/{transactionId}',
        body,
        {
          params: {
            path: { customerId, transactionId },
          },
        },
      ),

    onSuccess: async (_data: any, payload: any) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.customers.transactions(payload.customerId),
      })
    },
  })
}