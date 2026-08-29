import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type { ReportTemplateResponse } from '@/features/system/report/types/reportTemplateApiTypes'

export const useReportTemplatesQuery = () => {
  const query = useAppQuery<ReportTemplateResponse[]>({
    queryKey: queryKeys.reportTemplates.list(),
    queryFn: () => get<ReportTemplateResponse[]>('/api/system/report-templates'),
  })

  const templates = computed(() => query.data.value ?? [])

  return {
    ...query,
    templates,
  }
}
