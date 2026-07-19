import { computed, unref, type MaybeRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import type { CustomerDetailResponse } from '../types/customerApiTypes'
import { queryKeys } from './queryKeys'

export const useCustomerDetailQuery = (
  id: MaybeRef<number | null | undefined>,
) => {
  return useAppQuery<CustomerDetailResponse>({
    queryKey: computed(() => queryKeys.customers.detail(unref(id) as number)),
    enabled: computed(() => unref(id) != null),
    queryFn: () =>
      get<CustomerDetailResponse>('/api/customers/{id}', {
        params: {
          path: { id: unref(id) as number },
        },
      }),
  })
}