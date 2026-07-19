import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import type {
  DailyReportEstimatedPayPreviewResponse,
  DailyReportSaveRequest,
} from '@/features/dailyreport/types/dailyReportApiTypes'

export const useDailyReportEstimatedPayPreviewMutation = () => {
  return useAppMutation({
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