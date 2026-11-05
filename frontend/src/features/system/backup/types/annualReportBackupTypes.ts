export type AnnualReportBackupSetting = {
  fiscalYearStartMonth: number
  graceDays: number
  startupEnabled: boolean
  activeFlag: boolean
}

export type AnnualReportBackupResult = {
  executionId: number
  fiscalYear: number
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED'
  fileCount: number
  totalSize: number
  errorMessage: string | null
}
