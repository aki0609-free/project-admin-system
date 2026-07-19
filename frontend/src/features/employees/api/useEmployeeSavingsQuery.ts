import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import { EmployeeSavingResponse } from '../types/employeeWorkApiTypes'

export const useEmployeeSavingsQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.employeeWork.savings.list(),
    queryFn: async () =>
      await get<EmployeeSavingResponse[]>('/api/employee/savings'),
  })

  return {
    ...query,
    savings: computed(() => query.data.value ?? []),
  }
}