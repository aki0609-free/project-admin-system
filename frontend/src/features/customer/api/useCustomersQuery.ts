import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import type { CustomerListItemResponse } from '../types/customerApiTypes'
import { queryKeys } from './queryKeys'

export const useCustomersQuery = () => {
  const query = useAppQuery<CustomerListItemResponse[]>({
    queryKey: queryKeys.customers.list(),
    queryFn: () => get<CustomerListItemResponse[]>('/api/customers'),
  })

  return {
    ...query,
    customers: computed(() => query.data.value ?? []),
  }
}