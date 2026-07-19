import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { EmployeeLoanResponse, EmployeeLoanSaveRequest } from '../types/employeeWorkApiTypes'
import { queryKeys } from './queryKeys'

export const useCreateEmployeeLoanMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: EmployeeLoanSaveRequest) =>
      await post<EmployeeLoanResponse, EmployeeLoanSaveRequest>(
        '/api/employee/loans',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.employeeWork.loans.all,
      })
    },
  })
}