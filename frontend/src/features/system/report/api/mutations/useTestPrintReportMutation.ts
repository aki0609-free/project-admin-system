import { useAppMutation } from '@/shared/api/useAppMutation'
import { get, post } from '@/shared/api/http'
import type {
  ReportExecuteRequest,
  ReportExecuteResponse,
  ReportPreviewResponse,
} from '@/features/system/report/types/reportExecutionApiTypes'

type Payload = {
  reportCode: string
  params?: Record<string, unknown>
}

export type TestPrintResult = ReportExecuteResponse & {
  blob: Blob
  contentType: string
}

const base64ToBlob = (base64: string, contentType: string): Blob => {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)

  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i)
  }

  return new Blob([bytes], { type: contentType })
}

export const useTestPrintReportMutation = () => {
  return useAppMutation({
    mutationFn: async ({ reportCode, params }: Payload): Promise<TestPrintResult> => {
      const prepared = await post<ReportExecuteResponse, ReportExecuteRequest>(
        `/api/system/report-execution/${reportCode}/prepare`,
        {
          params: params ?? {},
        },
      )

      if (prepared.outputFormat === 'PDF') {
        const preview = await get<ReportPreviewResponse>(
          `/api/system/report-export/${reportCode}/preview?executionId=${encodeURIComponent(prepared.executionId)}`,
        )

        const blob = base64ToBlob(preview.base64Data, preview.contentType)

        return {
          ...prepared,
          blob,
          contentType: preview.contentType,
        }
      }

      const blob = await getBlobByFetch(
        `/api/system/report-export/${reportCode}/download?executionId=${encodeURIComponent(prepared.executionId)}`,
      )

      return {
        ...prepared,
        blob,
        contentType:
          prepared.outputFormat === 'CSV'
            ? 'text/csv'
            : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      }
    },
  })
}

const getBlobByFetch = async (url: string): Promise<Blob> => {
  const token = localStorage.getItem('accessToken')

  const response = await fetch(url, {
    method: 'GET',
    credentials: 'include',
    headers: token
      ? {
          Authorization: `Bearer ${token}`,
        }
      : {},
  })

  if (!response.ok) {
    throw new Error(`GET BLOB failed: ${response.status}`)
  }

  return await response.blob()
}