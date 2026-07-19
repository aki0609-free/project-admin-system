import { computed, type ComputedRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import type { EmployeeDetailResponse } from '../types/employeeApiTypes'
import { queryKeys } from './queryKeys'

export const useEmployeeDetailQuery = (
  id: ComputedRef<number | null>,
) => {
  const query = useAppQuery({
    queryKey: computed(() => queryKeys.employees.detail(id.value)),
    enabled: computed(() => id.value != null),
    queryFn: async () =>
      await get<EmployeeDetailResponse>(`/api/employees/${id.value}`),
  })

  return {
    ...query,
    employee: computed(() => query.data.value ?? null),
  }
}