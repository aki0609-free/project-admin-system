import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import type { CustomerSaveRequest } from '../types/customerApiTypes'
import { queryKeys } from './queryKeys'

export const useCreateCustomerMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (payload: CustomerSaveRequest) =>
      post<number, CustomerSaveRequest>('/api/customers', payload),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.customers.all,
      })
    },
  })
}