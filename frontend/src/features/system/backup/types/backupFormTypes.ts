import type {
  BackupDataType,
  BackupOutputMode,
} from '@/features/system/backup/types/backupApiTypes'

export type BackupColumnForm = {
  id: number
  columnName: string
  csvHeaderName: string
  dataType: BackupDataType
  exportFlag: boolean
  orderNo: number
}

export type BackupTargetDialogForm = {
  id: number
  targetCode: string
  targetName: string
  tableName: string
  description: string
  outputMode: BackupOutputMode
  outputDir: string
  fileNamePattern: string
  zipRequired: boolean
  backupEnabled: boolean
  activeFlag: boolean
  includeHeader: boolean
  columns: BackupColumnForm[]
}