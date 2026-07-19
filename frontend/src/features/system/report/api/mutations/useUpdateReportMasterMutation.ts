import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type {
  ReportMasterDetailResponse,
  ReportMasterSaveRequest,
} from '@/features/system/report/types/reportMasterApiTypes'

type Payload = {
  id: number
  request: ReportMasterSaveRequest
}

export const useUpdateReportMasterMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({ id, request }: Payload) =>
      put<ReportMasterDetailResponse, ReportMasterSaveRequest>(
        `/api/system/report-masters/${id}`,
        request,
      ),

    onSuccess: async (_data: any, variables: any) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.reportMasters.all,
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.reportMasters.detail(variables.id),
        }),
      ])
    },
  })
}