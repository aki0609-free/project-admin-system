export type OperationType = 'PREPARATION' | 'DAILY' | 'MONTHLY' | 'BOOK'

export type OperationReportOutputType =
  | 'NONE'
  | 'PDF'
  | 'CSV'
  | 'EXCEL'
  | 'EXCEL_BOOK'
  | 'CUSTOM'

export type OperationReportPreviewColumnResponse = {
  id: number
  previewName: string
  columnName: string
  displayOrder: number
}

export type OperationReportPreviewResponse = {
  id: number
  operationType: OperationType
  reportCode: string
  reportName: string
  jobCode: string | null
  tableName: string
  templateName: string
  displayOrder: number
  outputType: OperationReportOutputType
  columns: OperationReportPreviewColumnResponse[]
}