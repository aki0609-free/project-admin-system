/* eslint-disable @typescript-eslint/no-invalid-void-type */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import type { CustomerPaymentConfirmRequest } from '../types/customerApiTypes'
import { queryKeys } from './queryKeys'

export type ConfirmCustomerPaymentPayload = {
  customerId: number
  transactionId: number
  body: CustomerPaymentConfirmRequest
}

export const useConfirmCustomerPaymentMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({ customerId, transactionId, body }: ConfirmCustomerPaymentPayload) =>
      put<void, CustomerPaymentConfirmRequest>(
        '/api/customers/{customerId}/transactions/{transactionId}/confirm-payment',
        body,
        {
          params: {
            path: { customerId, transactionId },
          },
        },
      ),

    onSuccess: async (_data: any, payload: any) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.customers.transactions(payload.customerId),
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.customers.list(),
        }),
      ])
    },
  })
}