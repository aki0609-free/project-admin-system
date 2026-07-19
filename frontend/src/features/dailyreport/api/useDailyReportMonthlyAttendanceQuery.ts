import { computed, unref, type MaybeRef } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { queryKeys } from '@/features/dailyreport/api/queryKeys'
import type { DailyReportMonthlyAttendanceResponse } from '@/features/dailyreport/types/dailyReportApiTypes'

export const useDailyReportMonthlyAttendanceQuery = (
  targetMonth: MaybeRef<string>,
) => {
  const query = useAppQuery<DailyReportMonthlyAttendanceResponse[]>({
    queryKey: computed(() =>
      queryKeys.dailyReports.monthlyAttendance(unref(targetMonth)),
    ),

    enabled: computed(() => !!unref(targetMonth)),

    queryFn: async () =>
      await get<DailyReportMonthlyAttendanceResponse[]>(
        '/api/daily-reports/monthly-attendance',
        {
          params: {
            query: {
              targetMonth: unref(targetMonth),
            },
          },
        },
      ),
  })

  return {
    ...query,
    attendances: computed(() => query.data.value ?? []),
  }
}