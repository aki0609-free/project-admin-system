import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { EmployeeDetailResponse, EmployeeSaveRequest } from '../types/employeeApiTypes'
import { queryKeys } from './queryKeys'

type Payload = {
  id: number
  request: EmployeeSaveRequest
}

export const useUpdateEmployeeMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id, request }: Payload) =>
      await put<EmployeeDetailResponse, EmployeeSaveRequest>(
        `/api/employees/${id}`,
        request,
      ),

    onSuccess: async (_data: unknown, variables: Payload) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.employees.all,
      })

      await queryClient.invalidateQueries({
        queryKey: queryKeys.employees.detail(variables.id),
      })
    },
  })
}