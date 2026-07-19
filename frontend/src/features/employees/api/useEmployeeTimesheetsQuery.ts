import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import { EmployeeTimesheetResponse } from '../types/employeeWorkApiTypes'

export const useEmployeeTimesheetsQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.employeeWork.timesheets.list(),
    queryFn: async () =>
      await get<EmployeeTimesheetResponse[]>('/api/employee/timesheets'),
  })

  return {
    ...query,
    timesheets: computed(() => query.data.value ?? []),
  }
}