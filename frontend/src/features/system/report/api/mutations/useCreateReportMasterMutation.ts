/* eslint-disable @typescript-eslint/no-explicit-any */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type {
  ReportMasterDetailResponse,
  ReportMasterSaveRequest,
} from '@/features/system/report/types/reportMasterApiTypes'

export const useCreateReportMasterMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (request: ReportMasterSaveRequest) =>
      post<ReportMasterDetailResponse, ReportMasterSaveRequest>(
        '/api/system/report-masters',
        request,
      ),

    onSuccess: async (data: any) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.reportMasters.all,
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.reportMasters.detail(data.id),
        }),
      ])
    },
  })
}