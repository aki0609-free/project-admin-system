import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/dailyreport/api/queryKeys'
import type { DailyReportInputResponse } from '@/features/dailyreport/types/dailyReportInputItemTypes'

export const useDailyReportInputItemsQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.dailyReports.inputItems(),
    queryFn: async () =>
      await get<DailyReportInputResponse>('/api/daily-reports/input-items'),
  })

  const inputItems = computed(() => query.data.value ?? {
    allowances: [],
    deductions: [],
  })

  return {
    ...query,
    inputItems,
  }
}