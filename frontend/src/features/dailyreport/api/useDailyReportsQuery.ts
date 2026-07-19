import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/dailyreport/api/queryKeys'
import type { DailyReportResponse } from '@/features/dailyreport/types/dailyReportApiTypes'

export const useDailyReportsQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.dailyReports.list(),
    queryFn: async () =>
      await get<DailyReportResponse[]>('/api/daily-reports'),
  })

  return {
    ...query,
    reports: computed(() => query.data.value ?? []),
  }
}