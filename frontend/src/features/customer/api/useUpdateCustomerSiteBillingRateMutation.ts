/* eslint-disable @typescript-eslint/no-invalid-void-type */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import type {
  CustomerSiteBillingRateRequest,
} from '../types/customerApiTypes'

type UpdateCustomerSiteBillingRateVariables = {
  customerId: number
  billingRateId: number
  body: CustomerSiteBillingRateRequest
}

export const useUpdateCustomerSiteBillingRateMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({
      customerId,
      billingRateId,
      body,
    }: UpdateCustomerSiteBillingRateVariables) =>
      put<void, CustomerSiteBillingRateRequest>(
        '/api/customers/{customerId}/billing-rates/{billingRateId}',
        body,
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
      variables: UpdateCustomerSiteBillingRateVariables,
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