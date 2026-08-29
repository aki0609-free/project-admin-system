import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import type { CustomerSaveRequest } from '../types/customerApiTypes'
import { useCustomerMasterStore } from '../store/useCustomerMasterStore'
import { queryKeys } from './queryKeys'

export const useCreateCustomerMutation = () => {
  const queryClient = useQueryClient()
  const customerMasterStore = useCustomerMasterStore()

  return useAppMutation({
    mutationFn: (payload: CustomerSaveRequest) =>
      post<number, CustomerSaveRequest>('/api/customers', payload),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.customers.all,
      })
      await customerMasterStore.refresh()
    },
  })
}
