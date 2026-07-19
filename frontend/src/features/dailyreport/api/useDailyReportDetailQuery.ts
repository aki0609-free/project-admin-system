import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/dailyreport/api/queryKeys'
import type { DailyReportDetailResponse } from '@/features/dailyreport/types/dailyReportApiTypes'

export const useDailyReportDetailQuery = (
  id: () => number | null,
) => {
  const query = useAppQuery({
    queryKey: computed(() =>
      queryKeys.dailyReports.detail(id() ?? 0),
    ),

    enabled: computed(() => id() != null),

    queryFn: async () =>
      await get<DailyReportDetailResponse>(
        `/api/daily-reports/${id()}`,
      ),
  })

  return {
    ...query,
    dailyReport: computed(() => query.data.value ?? null),
  }
}