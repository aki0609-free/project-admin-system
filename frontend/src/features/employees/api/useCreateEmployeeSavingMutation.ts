import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { EmployeeSavingResponse, EmployeeSavingSaveRequest } from '../types/employeeWorkApiTypes'
import { queryKeys } from './queryKeys'

export const useCreateEmployeeSavingMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: EmployeeSavingSaveRequest) =>
      await post<EmployeeSavingResponse, EmployeeSavingSaveRequest>(
        '/api/employee/savings',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.employeeWork.savings.all,
      })
    },
  })
}