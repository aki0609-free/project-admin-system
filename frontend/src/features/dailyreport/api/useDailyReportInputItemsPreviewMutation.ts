import { useMutation } from '@tanstack/vue-query'

import { post } from '@/shared/api/http'
import type { DailyReportSaveRequest } from '@/features/dailyreport/types/dailyReportApiTypes'
import type { DailyReportInputResponse } from '@/features/dailyreport/types/dailyReportInputItemTypes'

export const useDailyReportInputItemsPreviewMutation = () => {
  return useMutation({
    mutationFn: async (request: DailyReportSaveRequest) =>
      await post<DailyReportInputResponse, DailyReportSaveRequest>(
        '/api/daily-reports/input-items/preview',
        request,
      ),
  })
}
