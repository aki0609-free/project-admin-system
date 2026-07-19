import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import type {
  EmployeeDetailResponse,
  EmployeeResignRequest,
} from '../types/employeeApiTypes'

type Payload = {
  id: number
  request: EmployeeResignRequest
}

export const useResignEmployeeMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id, request }: Payload) =>
      await post<EmployeeDetailResponse, EmployeeResignRequest>(
        `/api/employees/${id}/resign`,
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