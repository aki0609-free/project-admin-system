import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type { ReportHistoryResponse } from '@/features/system/report/types/reportHistoryApiTypes'

export const useReportHistoriesQuery = () => {
  const query = useAppQuery<ReportHistoryResponse[]>({
    queryKey: queryKeys.reportHistories.list(),
    queryFn: () => get<ReportHistoryResponse[]>('/api/system/report-histories'),
  })

  const reportHistories = computed(() => query.data.value ?? [])

  return {
    ...query,
    reportHistories,
  }
}