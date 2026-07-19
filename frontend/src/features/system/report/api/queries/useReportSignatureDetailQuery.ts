import { computed, unref, type MaybeRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type { ReportSignatureResponse } from '@/features/system/report/types/reportSignatureApiTypes'

export const useReportSignatureDetailQuery = (id: MaybeRef<number | null>) => {
  const resolvedId = computed(() => unref(id))

  const query = useAppQuery<ReportSignatureResponse>({
    queryKey: computed(() => queryKeys.reportSignatures.detail(resolvedId.value)),
    enabled: computed(() => resolvedId.value != null),
    queryFn: () => get<ReportSignatureResponse>(`/api/system/report-signatures/${resolvedId.value}`),
  })

  const reportSignature = computed(() => query.data.value ?? null)

  return {
    ...query,
    reportSignature,
  }
}