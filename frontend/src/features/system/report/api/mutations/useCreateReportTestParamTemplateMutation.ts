/* eslint-disable @typescript-eslint/no-explicit-any */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/report/api/queryKeys'
import type {
  ReportTestParamTemplateResponse,
  ReportTestParamTemplateSaveRequest,
} from '@/features/system/report/types/reportTestParamTemplateApiTypes'

export const useCreateReportTestParamTemplateMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: ReportTestParamTemplateSaveRequest) => {
      return await post<
        ReportTestParamTemplateResponse,
        ReportTestParamTemplateSaveRequest
      >('/api/system/report-test-param-templates', request)
    },
    onSuccess: async (_: any, request: any) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.reportTestParamTemplates.list(request.reportCode),
      })
    },
  })
}