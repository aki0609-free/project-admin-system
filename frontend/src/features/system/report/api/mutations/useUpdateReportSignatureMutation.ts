import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type {
  ReportSignatureResponse,
  ReportSignatureSaveRequest,
} from '@/features/system/report/types/reportSignatureApiTypes'

type Payload = {
  id: number
  request: ReportSignatureSaveRequest
}

export const useUpdateReportSignatureMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: ({ id, request }: Payload) =>
      put<ReportSignatureResponse, ReportSignatureSaveRequest>(
        `/api/system/report-signatures/${id}`,
        request,
      ),

    onSuccess: async (_data: any, variables: any) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.reportSignatures.all,
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.reportSignatures.byReportMaster(
            variables.request.reportMasterId ?? null,
          ),
        }),
      ])
    },
  })
}