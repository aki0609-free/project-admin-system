import { computed, unref, type MaybeRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type { ReportMasterDetailResponse } from '@/features/system/report/types/reportMasterApiTypes'

export const useReportMasterDetailQuery = (id: MaybeRef<number | null>) => {
  const query = useAppQuery<ReportMasterDetailResponse>({
    queryKey: computed(() => queryKeys.reportMasters.detail(unref(id))),
    queryFn: () => get<ReportMasterDetailResponse>(`/api/system/report-masters/${unref(id)}`),
    enabled: computed(() => unref(id) != null),
  })

  const reportMaster = computed(() => query.data.value ?? null)
  const params = computed(() => query.data.value?.params ?? [])

  return {
    ...query,
    reportMaster,
    params,
  }
}