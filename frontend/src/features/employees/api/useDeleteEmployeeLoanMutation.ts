import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from './queryKeys'

export const useDeleteEmployeeLoanMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (id: number) =>
      await del<void>(`/api/employee/loans/${id}`),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.employeeWork.loans.all,
      })
    },
  })
}