/* eslint-disable @typescript-eslint/no-invalid-void-type */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from './queryKeys'

type DeleteCustomerSiteBillingRateVariables = {
  customerId: number
  billingRateId: number
}

export const useDeleteCustomerSiteBillingRateMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({
      customerId,
      billingRateId,
    }: DeleteCustomerSiteBillingRateVariables) =>
      del<void>(
        '/api/customers/{customerId}/billing-rates/{billingRateId}',
        {
          params: {
            path: {
              customerId,
              billingRateId,
            },
          },
        },
      ),

    onSuccess: async (
      _: any,
      variables: DeleteCustomerSiteBillingRateVariables,
    ) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.customers.billingRates(
          variables.customerId,
        ),
      })

      await queryClient.invalidateQueries({
        queryKey: queryKeys.customers.detail(
          variables.customerId,
        ),
      })

      await queryClient.invalidateQueries({
        queryKey: queryKeys.customers.all,
      })
    },
  })
}