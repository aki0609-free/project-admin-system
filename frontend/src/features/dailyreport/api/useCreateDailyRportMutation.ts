import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/dailyreport/api/queryKeys'
import type {
  DailyReportResponse,
  DailyReportSaveRequest,
} from '@/features/dailyreport/types/dailyReportApiTypes'

export const useCreateDailyReportMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: DailyReportSaveRequest) =>
      await post<DailyReportResponse, DailyReportSaveRequest>(
        '/api/daily-reports',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.dailyReports.all,
      })

      await queryClient.invalidateQueries({
        queryKey: queryKeys.dailyReportMissing.all,
      })

      await queryClient.invalidateQueries({
        queryKey: ['daily-report-monthly-attendance'],
      })
    },
  })
}