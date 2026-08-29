import { useQueryClient } from '@tanstack/vue-query'

import { post } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'

import { queryKeys } from './queryKeys'

import type {
  CustomerSiteBillingRateBulkSaveRequest,
} from '../types/customerApiTypes'

type BulkSaveVariables = {
  customerId: number
  body: CustomerSiteBillingRateBulkSaveRequest
}

export const useBulkSaveCustomerSiteBillingRatesMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({
      customerId,
      body,
    }: BulkSaveVariables) =>
      post<unknown, CustomerSiteBillingRateBulkSaveRequest>(
        '/api/customers/{customerId}/billing-rates/bulk-save',
        body,
        {
          params: {
            path: {
              customerId,
            },
          },
        },
      ),

    onSuccess: async (
      _result: unknown,
      variables: BulkSaveVariables,
    ) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.customers.billingRates(
            variables.customerId,
          ),
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.customers.detail(
            variables.customerId,
          ),
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.customers.all,
        }),
      ])
    },
  })
}
