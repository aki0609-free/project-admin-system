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

export type PayrollPolicySetting = {
  id: number | null
  effectiveFrom: string
  effectiveTo: string | null
  weekStartDay:
    | 'MONDAY'
    | 'TUESDAY'
    | 'WEDNESDAY'
    | 'THURSDAY'
    | 'FRIDAY'
    | 'SATURDAY'
    | 'SUNDAY'
  weeklyStatutoryHours: number
  monthlyOvertimeThresholdHours: number
  overtimeRate: number
  overtimeOverThresholdRate: number
  nightPremiumRate: number
  statutoryHolidayRate: number
  dailyStandardHours: number
  amountRoundingMode: 'HALF_UP' | 'UP' | 'DOWN'
  activeFlag: boolean
}

export type PreviewReportSetting = {
  id: number | null
  operationType: 'PREPARATION' | 'DAILY' | 'MONTHLY' | 'BOOK'
  reportCode: string
  reportName: string
  tableName: string
  filterColumnName: string | null
  targetParamName: string | null
  orderBy: string | null
  displayOrder: number
  outputType: 'HTML_PREVIEW' | 'HTML_PRINT'
  activeFlag: boolean
  htmlTemplateKey: string | null
  htmlTemplateVersion: number
  htmlTemplateHash: string | null
  templateExists: boolean
}

export type PreviewReportSettingSaveRequest = Pick<
  PreviewReportSetting,
  | 'id'
  | 'operationType'
  | 'reportCode'
  | 'reportName'
  | 'tableName'
  | 'filterColumnName'
  | 'targetParamName'
  | 'orderBy'
  | 'displayOrder'
  | 'outputType'
  | 'activeFlag'
>
