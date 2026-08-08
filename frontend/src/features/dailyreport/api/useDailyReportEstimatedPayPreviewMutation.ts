import { useMutation } from '@tanstack/vue-query'
import { post } from '@/shared/api/http'
import type {
  DailyReportEstimatedPayPreviewResponse,
  DailyReportSaveRequest,
} from '@/features/dailyreport/types/dailyReportApiTypes'

export const useDailyReportEstimatedPayPreviewMutation = () => {
  return useMutation({
    mutationFn: async (request: DailyReportSaveRequest) =>
      await post<
        DailyReportEstimatedPayPreviewResponse,
        DailyReportSaveRequest
      >(
        '/api/daily-reports/estimated-pay-preview',
        request,
      ),
  })
}
