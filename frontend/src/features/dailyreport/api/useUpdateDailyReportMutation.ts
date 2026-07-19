import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/dailyreport/api/queryKeys'
import type {
  DailyReportResponse,
  DailyReportSaveRequest,
} from '@/features/dailyreport/types/dailyReportApiTypes'

type Payload = {
  id: number
  request: DailyReportSaveRequest
}

export const useUpdateDailyReportMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id, request }: Payload) =>
      await put<DailyReportResponse, DailyReportSaveRequest>(
        `/api/daily-reports/${id}`,
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