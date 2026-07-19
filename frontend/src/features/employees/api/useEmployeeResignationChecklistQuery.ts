import { computed } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { queryKeys } from './queryKeys'
import type { EmployeeResignationChecklistResponse } from '../types/employeeApiTypes'

export const useEmployeeResignationChecklistQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.employees.resignationChecklist(),
    queryFn: async () =>
      await get<EmployeeResignationChecklistResponse[]>(
        '/api/employees/resignation-checklist',
      ),
  })

  return {
    ...query,
    checklist: computed(() => query.data.value ?? []),
  }
}