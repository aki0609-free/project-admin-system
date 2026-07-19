/* eslint-disable @typescript-eslint/no-explicit-any */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import type { CustomerTransactionRequest } from '../types/customerApiTypes'
import { queryKeys } from './queryKeys'

export type CreateCustomerTransactionPayload = {
  customerId: number
  body: CustomerTransactionRequest
}

export const useCreateCustomerTransactionMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({ customerId, body }: CreateCustomerTransactionPayload) =>
      post<number, CustomerTransactionRequest>(
        '/api/customers/{customerId}/transactions',
        body,
        {
          params: {
            path: { customerId },
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