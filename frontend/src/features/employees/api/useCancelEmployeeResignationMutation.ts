import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import type { EmployeeDetailResponse } from '../types/employeeApiTypes'

export const useCancelEmployeeResignationMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (id: number) =>
      await post<EmployeeDetailResponse>(
        `/api/employees/${id}/cancel-resignation`,
      ),
    onSuccess: async (_data: unknown, id: number) => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.employees.all })
      await queryClient.invalidateQueries({
        queryKey: queryKeys.employees.detail(id),
      })
    },
  })
}
