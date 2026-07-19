import { computed, unref, type MaybeRef } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import type { EmployeeFinanceSummaryResponse } from '@/features/employees/types/employeeFinanceTypes'

export const useEmployeeFinanceSummaryQuery = (
  employeeId: MaybeRef<number | null>,
) => {
  const query = useAppQuery<EmployeeFinanceSummaryResponse>({
    queryKey: computed(() => [
      'employee-finance-summary',
      unref(employeeId),
    ]),
    enabled: computed(() => unref(employeeId) != null),
    queryFn: () =>
      get<EmployeeFinanceSummaryResponse>(
        '/api/employees/{employeeId}/finance-summary',
        {
          params: {
            path: {
              employeeId: unref(employeeId) as number,
            },
          },
        },
      ),
  })

  return {
    ...query,
    summary: computed(() => query.data.value ?? null),
  }
}