import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type { ReportTemplateListItemResponse } from '@/features/system/report/types/reportMasterApiTypes'

export const useReportTemplatesQuery = () => {
  const query = useAppQuery<ReportTemplateListItemResponse[]>({
    queryKey: queryKeys.reportTemplates.list(),
    queryFn: () => get<ReportTemplateListItemResponse[]>('/api/system/report-templates'),
  })

  const templates = computed(() => query.data.value ?? [])

  return {
    ...query,
    templates,
  }
}