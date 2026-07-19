import type { BackupTargetResponse } from '@/features/system/backup/types/backupApiTypes'
import type {
  BackupColumnForm,
  BackupTargetDialogForm,
} from '@/features/system/backup/types/backupFormTypes'

let tempColumnId = -1

export const createEmptyBackupColumnForm = (
  orderNo = 1,
): BackupColumnForm => ({
  id: tempColumnId--,
  columnName: '',
  csvHeaderName: '',
  dataType: 'STRING',
  exportFlag: true,
  orderNo,
})

export const createEmptyBackupTargetForm =
  (): BackupTargetDialogForm => ({
    id: 0,
    targetCode: '',
    targetName: '',
    tableName: '',
    description: '',
    outputMode: 'DOWNLOAD',
    outputDir: '',
    fileNamePattern: '',
    zipRequired: false,
    backupEnabled: true,
    activeFlag: true,
    includeHeader: true,
    columns: [],
  })

export const toBackupTargetDialogForm = (
  target: BackupTargetResponse,
): BackupTargetDialogForm => ({
  id: target.id,
  targetCode: target.targetCode,
  targetName: target.targetName,
  tableName: target.tableName,
  description: target.description ?? '',
  outputMode: target.outputMode,
  outputDir: target.outputDir ?? '',
  fileNamePattern: target.fileNamePattern ?? '',
  zipRequired: target.zipRequired,
  backupEnabled: target.backupEnabled,
  activeFlag: target.activeFlag,
  includeHeader: target.includeHeader,
  columns: (target.columns ?? []).map(column => ({
    id: column.id,
    columnName: column.columnName,
    csvHeaderName: column.csvHeaderName,
    dataType: column.dataType,
    exportFlag: column.exportFlag,
    orderNo: column.orderNo,
  })),
})