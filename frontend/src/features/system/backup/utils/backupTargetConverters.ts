import type { BackupTargetSaveRequest } from '@/features/system/backup/types/backupApiTypes'
import type { BackupTargetDialogForm } from '@/features/system/backup/types/backupFormTypes'

const blankToNull = (value: string): string | null =>
  value.trim() ? value.trim() : null

export const toBackupTargetSaveRequest = (
  form: BackupTargetDialogForm,
): BackupTargetSaveRequest => ({
  targetCode: form.targetCode.trim(),
  targetName: form.targetName.trim(),
  tableName: form.tableName.trim(),
  description: blankToNull(form.description),
  outputMode: form.outputMode,
  outputDir: blankToNull(form.outputDir),
  fileNamePattern: blankToNull(form.fileNamePattern),
  zipRequired: form.zipRequired,
  backupEnabled: form.backupEnabled,
  activeFlag: form.activeFlag,
  includeHeader: form.includeHeader,
  columns: form.columns
    .slice()
    .sort((a, b) => a.orderNo - b.orderNo)
    .map(column => ({
      id: column.id > 0 ? column.id : null,
      columnName: column.columnName.trim(),
      csvHeaderName: column.csvHeaderName.trim(),
      dataType: column.dataType,
      exportFlag: column.exportFlag,
      orderNo: column.orderNo,
    })),
})