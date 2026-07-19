import { computed, unref, type MaybeRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type { ReportHistoryDetailResponse } from '@/features/system/report/types/reportHistoryApiTypes'

export const useReportHistoryDetailQuery = (id: MaybeRef<number | null>) => {
  const query = useAppQuery<ReportHistoryDetailResponse>({
    queryKey: computed(() => queryKeys.reportHistories.detail(unref(id))),
    queryFn: () => get<ReportHistoryDetailResponse>(`/api/system/report-histories/${unref(id)}`),
    enabled: computed(() => unref(id) != null),
  })

  const history = computed(() => query.data.value ?? null)

  return {
    ...query,
    history,
  }
}