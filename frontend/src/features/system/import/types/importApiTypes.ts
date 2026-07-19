export type ImportSourceType = 
  | 'UPLOAD' 
  | 'SERVER_FILE' 
  | 'SCRIPT'

export type ImportHistoryStatus =
  | 'SUCCESS'
  | 'PARTIAL_SUCCESS'
  | 'FAILED'

export type ImportScriptType = 
  | 'NONE' 
  | 'SHELL' 
  | 'PYTHON'

export type ImportMode = 
  | 'INSERT_ONLY'
  | 'UPDATE_ONLY' 
  | 'UPSERT'
  | 'DELETE_INSERT'
  

export type ImportDataType =
  | 'STRING'
  | 'INTEGER'
  | 'LONG'
  | 'DECIMAL'
  | 'BOOLEAN'
  | 'DATE'
  | 'DATETIME'

export type ImportColumnResponse = {
  id: number
  columnName: string
  csvHeaderName: string
  dataType: ImportDataType
  requiredFlag: boolean
  keyFlag: boolean
  nullableFlag: boolean
  trimFlag: boolean
  defaultValue: string | null
  formatPattern: string | null
  updatableFlag: boolean
  orderNo: number
}

export type ImportTargetResponse = {
  id: number
  targetCode: string
  targetName: string
  tableName: string
  description: string | null
  sourceType: ImportSourceType
  fixedFilePath: string | null
  scriptType: ImportScriptType
  scriptPath: string | null
  scriptArgs: string | null
  importMode: ImportMode
  headerRowNumber: number
  dataStartRowNumber: number
  charset: string
  delimiter: string
  activeFlag: boolean
  columns?: ImportColumnResponse[]
}

export type ImportColumnSaveRequest = {
  id: number | null
  columnName: string
  csvHeaderName: string
  dataType: ImportDataType
  requiredFlag: boolean
  keyFlag: boolean
  nullableFlag: boolean
  trimFlag: boolean
  defaultValue: string | null
  formatPattern: string | null
  updatableFlag: boolean
  orderNo: number
}

export type ImportTargetSaveRequest = {
  targetCode: string
  targetName: string
  tableName: string
  description: string | null
  sourceType: ImportSourceType
  fixedFilePath: string | null
  scriptType: ImportScriptType
  scriptPath: string | null
  scriptArgs: string | null
  importMode: ImportMode
  headerRowNumber: number
  dataStartRowNumber: number
  charset: string
  delimiter: string
  activeFlag: boolean
  columns: ImportColumnSaveRequest[]
}

export type ImportExecuteResponse = {
  targetCode: string
  fileName: string
  jobExecutionId: number
  status: string
  message: string
}

export type ImportScriptFileResponse = {
  fileName: string
  filePath: string
  extension: string
}

export type ImportHistoryResponse = {
  id: number
  targetCode: string
  targetName: string | null
  tableName: string | null
  sourceType: ImportSourceType | null
  importMode: ImportMode | null
  fileName: string | null
  totalCount: number
  insertedCount: number
  updatedCount: number
  skippedCount: number
  errorCount: number
  status: ImportHistoryStatus
  jobExecutionId: number | null
  executedBy: string | null
  executedAt: string
  errorMessage: string | null
}

export type ImportErrorRowResponse = {
  id: number
  rowNo: number
  csvHeaderName: string | null
  columnName: string | null
  rawValue: string | null
  errorMessage: string
}