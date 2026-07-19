import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type {
  ReportSignatureResponse,
  ReportSignatureSaveRequest,
} from '@/features/system/report/types/reportSignatureApiTypes'

export const useCreateReportSignatureMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (request: ReportSignatureSaveRequest) =>
      post<ReportSignatureResponse, ReportSignatureSaveRequest>(
        '/api/system/report-signatures',
        request,
      ),

    onSuccess: async (_data: any, variables: any) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.reportSignatures.all,
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.reportSignatures.byReportMaster(
            variables.reportMasterId ?? null,
          ),
        }),
      ])
    },
  })
}