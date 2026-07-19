import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { EmployeeTimesheetResponse, EmployeeTimesheetSaveRequest } from '../types/employeeWorkApiTypes'
import { queryKeys } from './queryKeys'

type Payload = {
  id: number
  request: EmployeeTimesheetSaveRequest
}

export const useUpdateEmployeeTimesheetMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id, request }: Payload) =>
      await put<EmployeeTimesheetResponse, EmployeeTimesheetSaveRequest>(
        `/api/employee/timesheets/${id}`,
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.employeeWork.timesheets.all,
      })
    },
  })
}