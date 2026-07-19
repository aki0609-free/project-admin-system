/* eslint-disable @typescript-eslint/no-explicit-any */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import type {
  CustomerSiteBillingRateRequest,
} from '../types/customerApiTypes'

type CreateCustomerSiteBillingRateVariables = {
  customerId: number
  body: CustomerSiteBillingRateRequest
}

export const useCreateCustomerSiteBillingRateMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({
      customerId,
      body,
    }: CreateCustomerSiteBillingRateVariables) =>
      post<number, CustomerSiteBillingRateRequest>(
        '/api/customers/{customerId}/billing-rates',
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
      _: any,
      variables: CreateCustomerSiteBillingRateVariables,
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