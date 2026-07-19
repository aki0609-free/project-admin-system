import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type { ReportMasterListItemResponse } from '@/features/system/report/types/reportMasterApiTypes'

export const useReportMastersQuery = () => {
  const query = useAppQuery<ReportMasterListItemResponse[]>({
    queryKey: queryKeys.reportMasters.list(),
    queryFn: () => get<ReportMasterListItemResponse[]>('/api/system/report-masters'),
  })

  const reportMasters = computed(() => query.data.value ?? [])

  return {
    ...query,
    reportMasters,
  }
}