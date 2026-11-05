export type MonthlyClosingOutputSetting = {
  id: number | null
  reportCode: string
  reportName: string | null
  jobCode: string | null
  outputType: 'NONE' | 'HTML_PREVIEW' | 'HTML_PRINT' | 'PDF' | 'CSV' | 'EXCEL' | 'EXCEL_BOOK' | 'CUSTOM'
  executionOrder: number
  requiredFlag: boolean
  activeFlag: boolean
  backupRetentionYears: number | null
}

export type MonthlyClosingOutputSaveRequest = Pick<
  MonthlyClosingOutputSetting,
  'reportCode' | 'executionOrder' | 'activeFlag' | 'backupRetentionYears'
>

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
