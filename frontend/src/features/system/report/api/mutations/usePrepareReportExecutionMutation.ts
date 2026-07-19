import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import type {
  ReportExecuteRequest,
  ReportExecuteResponse,
} from '@/features/system/report/types/reportExecutionApiTypes'

type Payload = {
  reportCode: string
  request: ReportExecuteRequest
}

export const usePrepareReportExecutionMutation = () => {
  return useAppMutation({
    mutationFn: ({ reportCode, request }: Payload) =>
      post<ReportExecuteResponse, ReportExecuteRequest>(
        `/api/system/report-execution/${reportCode}/prepare`,
        request,
      ),
  })
}