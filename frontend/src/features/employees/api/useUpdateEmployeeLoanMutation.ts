import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { EmployeeLoanResponse, EmployeeLoanSaveRequest } from '../types/employeeWorkApiTypes'
import { queryKeys } from './queryKeys'

type Payload = {
  id: number
  request: EmployeeLoanSaveRequest
}

export const useUpdateEmployeeLoanMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id, request }: Payload) =>
      await put<EmployeeLoanResponse, EmployeeLoanSaveRequest>(
        `/api/employee/loans/${id}`,
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.employeeWork.loans.all,
      })
    },
  })
}