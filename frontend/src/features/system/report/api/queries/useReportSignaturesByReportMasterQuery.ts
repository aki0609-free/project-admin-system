import { computed, unref, type MaybeRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type { ReportSignatureResponse } from '@/features/system/report/types/reportSignatureApiTypes'

export const useReportSignaturesByReportMasterQuery = (
  reportMasterId: MaybeRef<number | null>,
) => {
  const query = useAppQuery<ReportSignatureResponse[]>({
    queryKey: computed(() =>
      queryKeys.reportSignatures.byReportMaster(unref(reportMasterId)),
    ),
    queryFn: () =>
      get<ReportSignatureResponse[]>(
        `/api/system/report-signatures/by-report-master/${unref(reportMasterId)}`,
      ),
    enabled: computed(() => unref(reportMasterId) != null),
  })

  const signatures = computed(() => query.data.value ?? [])

  return {
    ...query,
    signatures,
  }
}