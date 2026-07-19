import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import type { CustomerOptionResponse } from '../types/customerApiTypes'

export const useCustomerOptionsQuery = () => {
  const query = useAppQuery({
    queryKey: ['customers', 'options'],
    queryFn: () =>
      get<CustomerOptionResponse>(
        '/api/customers/options',
      ),
    staleTime: Infinity,
  })

  return {
    ...query,
    options: computed(() => query.data.value),
  }
}