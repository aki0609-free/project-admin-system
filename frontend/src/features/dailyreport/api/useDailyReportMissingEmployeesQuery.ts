import { computed, unref, type MaybeRef } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { queryKeys } from '@/features/dailyreport/api/queryKeys'
import type { DailyReportMissingEmployeeResponse } from '@/features/dailyreport/types/dailyReportApiTypes'

export const useDailyReportMissingEmployeesQuery = (
  workDate: MaybeRef<string>,
) => {
  const query = useAppQuery<DailyReportMissingEmployeeResponse[]>({
    queryKey: computed(() =>
      queryKeys.dailyReportMissing.list(unref(workDate)),
    ),

    enabled: computed(() => !!unref(workDate)),

    queryFn: async () =>
      await get<DailyReportMissingEmployeeResponse[]>(
        '/api/daily-reports/missing',
        {
          params: {
            query: {
              workDate: unref(workDate),
            },
          },
        },
      ),
  })

  return {
    ...query,
    employees: computed(() => query.data.value ?? []),
  }
}