/* eslint-disable @typescript-eslint/no-invalid-void-type */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/dailyreport/api/queryKeys'

export const useDeleteDailyReportMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (id: number) =>
      await del<void>(`/api/daily-reports/${id}`),

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