import type { DayRule } from '@/shared/types/dayRuleTypes'

export type ResignationMessage = {
  dialogTitle: string
  guidanceMessage: string
  confirmationMessage: string
}

export type ResignationChecklistItem = {
  id: number
  code: string
  name: string
  description: string | null
  requiredFlag: boolean
  displayOrder: number
  activeFlag: boolean
}

export type ResignationChecklistSaveRequest = Omit<ResignationChecklistItem, 'id'>

export type BusinessClosingSetting = {
  id: number | null
  settingCode: string
  closingDay: DayRule
  paymentDay: DayRule
  activeFlag: boolean
}

export type MonthlyClosingOutputSetting = {
  id: number | null
  reportCode: string
  reportName: string | null
  jobCode: string | null
  outputType:
    | 'NONE'
    | 'HTML_PREVIEW'
    | 'HTML_PRINT'
    | 'PDF'
    | 'CSV'
    | 'EXCEL'
    | 'EXCEL_BOOK'
    | 'CUSTOM'
  executionOrder: number
  requiredFlag: boolean
  activeFlag: boolean
  backupRetentionYears: number | null
}

export type MonthlyClosingOutputSaveRequest = Pick<
  MonthlyClosingOutputSetting,
  'reportCode' | 'executionOrder' | 'activeFlag' | 'backupRetentionYears'
>

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

export type ExternalSupportLinkSetting = {
  incidentReportUrl: string
  manualUrl: string
}
