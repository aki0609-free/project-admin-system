import { useAppMutation } from '@/shared/api/useAppMutation'
import { postBlob } from '@/shared/api/http'

type Payload = {
  reportCode: string
  executionId: string
  format: 'PDF' | 'CSV' | 'EXCEL'
}

export const useDownloadReportMutation = () => {
  return useAppMutation({
    mutationFn: ({ reportCode, executionId, format }: Payload) =>
      postBlob(
        `/api/system/report-execution/${reportCode}/download`,
        { executionId, format },
      ),
  })
}