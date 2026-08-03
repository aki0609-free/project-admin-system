export type ExcelBookSourceType = 'SNAPSHOT'
export type ExcelBookLayoutType =
  | 'REPEATING_ROW'
  | 'MONTHLY_SUMMARY'
  | 'DEDICATED'
export type ExcelBookVariableScope = 'CONTEXT' | 'ROW'
export type ExcelBookVariableDataType =
  | 'STRING'
  | 'NUMBER'
  | 'DATE'
  | 'DATETIME'
  | 'BOOLEAN'
export type ExcelBookSelectionMode = 'NONE' | 'SINGLE' | 'MULTIPLE'
export type ExcelBookGenerationUnit =
  | 'ONE_FILE'
  | 'FILE_PER_SELECTION'
export type ExcelBookPrintOrientation = 'PORTRAIT' | 'LANDSCAPE'

export type ExcelBookSelectionConfig = {
  mode: ExcelBookSelectionMode
  dataSourceCode: string | null
  valueColumn: string | null
  displayColumns: string[]
  allowSelectAll: boolean
  generationUnit: ExcelBookGenerationUnit
}

export type ExcelBookPrintConfig = {
  paperSize: 'A3' | 'A4' | 'B5'
  orientation: ExcelBookPrintOrientation
  fitToOnePage: boolean
}

export type ExcelBookVariableMapping = {
  id: number | null
  variableKey: string
  sourceColumn: string
  scope: ExcelBookVariableScope
  dataType: ExcelBookVariableDataType
  orderNo: number
}

export type ExcelBookDataSourceCatalogColumn = {
  columnName: string
  displayName: string
  dataType: ExcelBookVariableDataType
  orderNo: number
}

export type ExcelBookDataSourceCatalog = {
  sourceCode: string
  displayName: string
  description: string | null
  columns: ExcelBookDataSourceCatalogColumn[]
}

export type ExcelBookMasterResponse = {
  id: number
  bookCode: string
  bookName: string
  sourceType: ExcelBookSourceType
  layoutType: ExcelBookLayoutType
  rendererKey: string
  selection: ExcelBookSelectionConfig
  print: ExcelBookPrintConfig
  dataSourceCode: string
  templateSheetName: string
  activeFlag: boolean
  variableMappings: ExcelBookVariableMapping[]
}

export type ExcelBookMasterRequest = {
  bookCode: string
  bookName: string
  sourceType: ExcelBookSourceType
  layoutType: ExcelBookLayoutType
  rendererKey: string
  selection: ExcelBookSelectionConfig
  print: ExcelBookPrintConfig
  dataSourceCode: string
  templateSheetName: string
  activeFlag: boolean
  variableMappings: Omit<ExcelBookVariableMapping, 'id'>[]
}

export type ExcelBookMasterForm = ExcelBookMasterResponse & {
  _isNew?: boolean
}

export type ExcelBookUpdateRequest = {
  targetMonth: string
}

export type SpreadsheetWorkbook = Record<string, unknown>

export type SpreadsheetTemplateResponse = {
  masterId: number
  bookCode: string
  storagePath: string
  workbook: SpreadsheetWorkbook | null
}

export type SpreadsheetTemplateSaveRequest = {
  workbook: SpreadsheetWorkbook
}

export type SpreadsheetJsonResult = {
  jsonObject: SpreadsheetWorkbook
}
