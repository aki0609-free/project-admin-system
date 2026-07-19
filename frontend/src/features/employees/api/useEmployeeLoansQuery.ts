import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import { EmployeeLoanResponse } from '../types/employeeWorkApiTypes'

export const useEmployeeLoansQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.employeeWork.loans.list(),
    queryFn: async () =>
      await get<EmployeeLoanResponse[]>('/api/employee/loans'),
  })

  return {
    ...query,
    loans: computed(() => query.data.value ?? []),
  }
}