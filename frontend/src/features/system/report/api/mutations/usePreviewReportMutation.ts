import { useAppMutation } from '@/shared/api/useAppMutation'
import { postBlob } from '@/shared/api/http'

type Payload = {
  reportCode: string
  executionId: string
}

export const usePreviewReportMutation = () => {
  return useAppMutation({
    mutationFn: ({ reportCode, executionId }: Payload) =>
      postBlob(
        `/api/system/report-execution/${reportCode}/preview`,
        { executionId },
      ),
  })
}