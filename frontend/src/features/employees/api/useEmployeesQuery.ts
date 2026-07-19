import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import { EmployeeListItemResponse } from '../types/employeeApiTypes'

export const useEmployeesQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.employees.list(),
    queryFn: async () =>
      await get<EmployeeListItemResponse[]>('/api/employees'),
  })

  return {
    ...query,
    employees: computed(() => query.data.value ?? []),
  }
}