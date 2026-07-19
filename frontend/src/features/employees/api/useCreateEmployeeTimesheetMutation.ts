import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { EmployeeTimesheetResponse, EmployeeTimesheetSaveRequest } from '../types/employeeWorkApiTypes'
import { queryKeys } from './queryKeys'

export const useCreateEmployeeTimesheetMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: EmployeeTimesheetSaveRequest) =>
      await post<EmployeeTimesheetResponse, EmployeeTimesheetSaveRequest>(
        '/api/employee/timesheets',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.employeeWork.timesheets.all,
      })
    },
  })
}