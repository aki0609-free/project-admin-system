import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { EmployeeSavingResponse, EmployeeSavingSaveRequest } from '../types/employeeWorkApiTypes'
import { queryKeys } from './queryKeys'

type Payload = {
  id: number
  request: EmployeeSavingSaveRequest
}

export const useUpdateEmployeeSavingMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id, request }: Payload) =>
      await put<EmployeeSavingResponse, EmployeeSavingSaveRequest>(
        `/api/employee/savings/${id}`,
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.employeeWork.savings.all,
      })
    },
  })
}