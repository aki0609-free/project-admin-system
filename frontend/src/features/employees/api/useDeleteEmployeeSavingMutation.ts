import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from './queryKeys'

export const useDeleteEmployeeSavingMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (id: number) =>
      await del<void>(`/api/employee/savings/${id}`),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.employeeWork.savings.all,
      })
    },
  })
}