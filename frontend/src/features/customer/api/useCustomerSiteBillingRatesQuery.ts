import {
  computed,
  unref,
  type MaybeRef,
} from 'vue'

import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import type {
  CustomerSiteBillingRateResponse,
} from '../types/customerApiTypes'

export const useCustomerSiteBillingRatesQuery = (
  customerId: MaybeRef<number | null | undefined>,
) => {
  const query = useAppQuery<
    CustomerSiteBillingRateResponse[]
  >({
    queryKey: computed(() =>
      queryKeys.customers.billingRates(
        unref(customerId) ?? -1,
      ),
    ),

    enabled: computed(() => {
      const id = unref(customerId)
      return id != null && id > 0
    }),

    queryFn: () =>
      get<CustomerSiteBillingRateResponse[]>(
        '/api/customers/{customerId}/billing-rates',
        {
          params: {
            path: {
              customerId: unref(customerId) as number,
            },
          },
        },
      ),
  })

  const billingRates = computed(
    () => query.data.value ?? [],
  )

  return {
    ...query,
    billingRates,
  }
}