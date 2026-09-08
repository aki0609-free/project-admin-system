import { computed, unref, type MaybeRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/dailyreport/api/queryKeys'
import type { DailyReportResponse } from '@/features/dailyreport/types/dailyReportApiTypes'

export const useDailyReportsQuery = (
  from: MaybeRef<string>,
  to: MaybeRef<string>,
) => {
  const query = useAppQuery({
    queryKey: computed(() =>
      queryKeys.dailyReports.list(unref(from), unref(to)),
    ),
    queryFn: async () => {
      const resolvedFrom = unref(from)
      const resolvedTo = unref(to)

      return await get<DailyReportResponse[]>('/api/daily-reports', {
        params: {
          query: {
            ...(resolvedFrom ? { from: resolvedFrom } : {}),
            ...(resolvedTo ? { to: resolvedTo } : {}),
          },
        },
      })
    },
  })

  return {
    ...query,
    reports: computed(() => query.data.value ?? []),
  }
}
