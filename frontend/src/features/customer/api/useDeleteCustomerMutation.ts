/* eslint-disable @typescript-eslint/no-invalid-void-type */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import { useCustomerMasterStore } from '../store/useCustomerMasterStore'

export const useDeleteCustomerMutation = () => {
  const queryClient = useQueryClient()
  const customerMasterStore = useCustomerMasterStore()

  return useAppMutation({
    mutationFn: (id: number) =>
      del<void>('/api/customers/{id}', {
        params: {
          path: { id },
        },
      }),

    onSuccess: async (_data: unknown, id: number) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.customers.all,
        }),
        queryClient.removeQueries({
          queryKey: queryKeys.customers.detail(id),
        }),
        customerMasterStore.refresh(),
      ])
    },
  })
}
