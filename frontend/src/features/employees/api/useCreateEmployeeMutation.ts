import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { EmployeeDetailResponse, EmployeeSaveRequest } from '../types/employeeApiTypes'
import { queryKeys } from './queryKeys'


export const useCreateEmployeeMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: EmployeeSaveRequest) =>
      await post<EmployeeDetailResponse, EmployeeSaveRequest>(
        '/api/employees',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.employees.all,
      })
    },
  })
}