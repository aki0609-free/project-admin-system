import { computed, unref, type MaybeRef } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import type { CustomerTransactionResponse } from '../types/customerApiTypes'
import { queryKeys } from './queryKeys'

export const useCustomerTransactionsQuery = (
  customerId: MaybeRef<number | null | undefined>,
) => {
  const query = useAppQuery<CustomerTransactionResponse[]>({
    queryKey: computed(() =>
      unref(customerId) == null
        ? [...queryKeys.customers.all, 'transactions', 'all']
        : queryKeys.customers.transactions(unref(customerId) as number),
    ),
    queryFn: () => {
      const id = unref(customerId)

      if (id == null) {
        return get<CustomerTransactionResponse[]>('/api/customer-transactions')
      }

      return get<CustomerTransactionResponse[]>(
        '/api/customers/{customerId}/transactions',
        {
          params: {
            path: { customerId: id },
          },
        },
      )
    },
  })

  return {
    ...query,
    transactions: computed(() => query.data.value ?? []),
  }
}