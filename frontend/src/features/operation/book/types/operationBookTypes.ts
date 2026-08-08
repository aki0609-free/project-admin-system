export type OperationExcelBook = {
  id: number
  bookCode: string
  bookName: string
  dataSourceCode: string
  generationMode: 'TEMPLATE' | 'CODE'
  generationReady: boolean
  templateConfigured: boolean
  selection: SpreadsheetLedgerSelectionConfig
  print: SpreadsheetLedgerPrintConfig
}

export type SpreadsheetLedgerSelectionMode =
  | 'NONE'
  | 'SINGLE'
  | 'MULTIPLE'

export type SpreadsheetLedgerSelectionConfig = {
  mode: SpreadsheetLedgerSelectionMode
  dataSourceCode: string | null
  valueColumn: string | null
  displayColumns: string[]
  allowSelectAll: boolean
  generationUnit: 'ONE_FILE' | 'FILE_PER_SELECTION'
}

export type SpreadsheetLedgerPrintConfig = {
  paperSize: 'A3' | 'A4' | 'B5'
  orientation: 'PORTRAIT' | 'LANDSCAPE'
  fitToOnePage: boolean
}

export type SpreadsheetLedgerGenerateRequest = {
  targetMonth: string
  selectionValues?: string[]
}

export type SpreadsheetLedgerGenerateResponse = {
  masterId: number
  bookCode: string
  bookName: string
  targetMonth: string
  rowCount: number
  generatedAt: string
  storagePath: string
  workbookBytes: number
  generationDurationMs: number
  editable: boolean
  workbook: Record<string, unknown>
  selectionValue: string | null
}

export type SpreadsheetLedgerSelectionColumn = {
  columnName: string
  displayName: string
  dataType: string
  orderNo: number
}

export type SpreadsheetLedgerSelectionOption = {
  value: string
  displayValues: Record<string, unknown>
}

export type SpreadsheetLedgerSelectionResponse = {
  mode: SpreadsheetLedgerSelectionMode
  valueColumn: string | null
  allowSelectAll: boolean
  generationUnit: 'ONE_FILE' | 'FILE_PER_SELECTION'
  columns: SpreadsheetLedgerSelectionColumn[]
  options: SpreadsheetLedgerSelectionOption[]
}

export type SpreadsheetLedgerSaveRequest = {
  workbook: Record<string, unknown>
}

export type SpreadsheetLedgerSaveResponse = {
  storagePath: string
  workbookBytes: number
  savedAt: string
}
