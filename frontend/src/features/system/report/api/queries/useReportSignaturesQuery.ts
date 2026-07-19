import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type { ReportSignatureResponse } from '@/features/system/report/types/reportSignatureApiTypes'

export const useReportSignaturesQuery = () => {
  const query = useAppQuery<ReportSignatureResponse[]>({
    queryKey: queryKeys.reportSignatures.list(),
    queryFn: () => get<ReportSignatureResponse[]>('/api/system/report-signatures'),
  })

  const reportSignatures = computed(() => query.data.value ?? [])

  return {
    ...query,
    reportSignatures,
  }
}