import { computed, unref, type MaybeRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type { ReportHistoryResponse } from '@/features/system/report/types/reportHistoryApiTypes'

export const useReportHistoriesByReportMasterQuery = (
  reportMasterId: MaybeRef<number | null>,
) => {
  const query = useAppQuery<ReportHistoryResponse[]>({
    queryKey: computed(() =>
      queryKeys.reportHistories.byReportMaster(unref(reportMasterId)),
    ),
    queryFn: () =>
      get<ReportHistoryResponse[]>(
        `/api/system/report-histories/by-report-master/${unref(reportMasterId)}`,
      ),
    enabled: computed(() => unref(reportMasterId) != null),
  })

  const histories = computed(() => query.data.value ?? [])

  return {
    ...query,
    histories,
  }
}