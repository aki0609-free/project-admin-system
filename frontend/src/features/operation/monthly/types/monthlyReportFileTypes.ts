export type MonthlyClosingReportFileResponse = {
  id: number
  reportCode: string
  targetType: string | null
  targetId: number | null
  targetName: string | null
  batchExecutionLogId: number | null
  storageType: 'LOCAL' | 'S3' | null
  outputFileKey: string | null
  outputFileName: string | null
  contentType: string | null
  fileSize: number | null
  generatedAt: string | null
}
