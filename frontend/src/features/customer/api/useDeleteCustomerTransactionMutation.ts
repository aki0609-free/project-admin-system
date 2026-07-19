/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/no-invalid-void-type */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from './queryKeys'

export type DeleteCustomerTransactionPayload = {
  customerId: number
  transactionId: number
}

export const useDeleteCustomerTransactionMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({ customerId, transactionId }: DeleteCustomerTransactionPayload) =>
      del<void>(
        '/api/customers/{customerId}/transactions/{transactionId}',
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