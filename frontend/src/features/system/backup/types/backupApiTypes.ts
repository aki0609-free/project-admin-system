export type StorageType = 'LOCAL' | 'S3'

export type BackupDataType =
  | 'STRING'
  | 'INTEGER'
  | 'LONG'
  | 'DECIMAL'
  | 'BOOLEAN'
  | 'DATE'
  | 'DATETIME'

export type BackupOutputMode =
  | 'DOWNLOAD'
  | 'SERVER_FILE'
  | 'BOTH'

export type BackupHistoryStatus =
  | 'SUCCESS'
  | 'FAILED'

export type BackupColumnResponse = {
  id: number
  columnName: string
  csvHeaderName: string
  dataType: BackupDataType
  exportFlag: boolean
  orderNo: number
}

export type BackupTargetResponse = {
  id: number
  targetCode: string
  targetName: string
  tableName: string
  description: string | null
  outputMode: BackupOutputMode
  outputDir: string | null
  fileNamePattern: string | null
  zipRequired: boolean
  backupEnabled: boolean
  activeFlag: boolean
  includeHeader: boolean
  columns?: BackupColumnResponse[]
}

export type BackupColumnSaveRequest = {
  id: number | null
  columnName: string
  csvHeaderName: string
  dataType: BackupDataType
  exportFlag: boolean
  orderNo: number
}

export type BackupTargetSaveRequest = {
  targetCode: string
  targetName: string
  tableName: string
  description: string | null
  outputMode: BackupOutputMode
  outputDir: string | null
  fileNamePattern: string | null
  zipRequired: boolean
  backupEnabled: boolean
  activeFlag: boolean
  includeHeader: boolean
  columns: BackupColumnSaveRequest[]
}

export type BackupExecuteRequest = {
  targetCodes: string[]
}

export type BackupHistoryResponse = {
  id: number
  targetCodes: string
  fileName: string | null
  contentType: string | null
  fileSize: number | null
  zipOutput: boolean

  storageType: StorageType | null
  storedFileKey: string | null
  storedFileName: string | null

  status: BackupHistoryStatus
  executedBy: string | null
  executedAt: string
  errorMessage: string | null
}