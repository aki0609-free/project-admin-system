import { computed, unref, type MaybeRef } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import type { EmployeeContractQueryResponse } from '@/features/employees/types/employeeWorkApiTypes'

export const useEmployeeContractQuery = (
  employeeId: MaybeRef<number | null>,
) => {
  const query = useAppQuery<EmployeeContractQueryResponse>({
    queryKey: computed(() => [
      'employee-contract',
      unref(employeeId),
    ]),

    enabled: computed(() => unref(employeeId) != null),

    queryFn: async () =>
      await get<EmployeeContractQueryResponse>(
        `/api/employees/${unref(employeeId)}/contract`,
      ),
  })

  return {
    ...query,
    contract: computed(() => query.data.value ?? null),
  }
}