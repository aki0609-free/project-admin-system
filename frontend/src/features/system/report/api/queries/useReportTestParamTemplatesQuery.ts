import { computed, type Ref } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type { ReportTestParamTemplateResponse } from '@/features/system/report/types/reportTestParamTemplateApiTypes'

export const useReportTestParamTemplatesQuery = (
  reportCode: Ref<string | null>,
) => {
  const query = useAppQuery({
    queryKey: computed(() =>
      queryKeys.reportTestParamTemplates.list(reportCode.value),
    ),
    enabled: computed(() => !!reportCode.value),
    queryFn: async () => {
      return await get<ReportTestParamTemplateResponse[]>(
        `/api/system/report-test-param-templates?reportCode=${encodeURIComponent(reportCode.value ?? '')}`,
      )
    },
  })

  return {
    ...query,
    templates: computed(() => query.data.value ?? []),
  }
}