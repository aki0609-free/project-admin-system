import type { ReportOutputFormat } from './reportEnums'

export type ReportExecuteRequest = {
  params?: Record<string, unknown>
}

export type ReportExecuteResponse = {
  reportCode: string
  executionId: string
  outputFormat: ReportOutputFormat
  fileName?: string | null
  previewEnabled?: boolean | null
  message?: string | null
}

export type ReportPreviewResponse = {
  reportCode: string
  executionId: string
  contentType: string
  fileName: string
  base64Data: string
}