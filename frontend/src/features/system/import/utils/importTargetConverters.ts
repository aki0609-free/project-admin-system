import type {
  ImportTargetSaveRequest,
} from '@/features/system/import/types/importApiTypes'
import type {
  ImportTargetDialogForm,
} from '@/features/system/import/components/ImportTargetEditDialog.vue'

const nullable = (value: string | null | undefined): string | null => {
  if (value == null) return null
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}

export const toImportTargetSaveRequest = (
  form: ImportTargetDialogForm,
): ImportTargetSaveRequest => ({
  targetCode: form.targetCode.trim(),
  targetName: form.targetName.trim(),
  tableName: form.tableName.trim(),
  description: nullable(form.description),
  sourceType: form.sourceType,
  fixedFilePath: nullable(form.fixedFilePath),
  scriptType: form.scriptType,
  scriptPath: nullable(form.scriptPath),
  scriptArgs: nullable(form.scriptArgs),
  importMode: form.importMode,
  headerRowNumber: form.headerRowNumber,
  dataStartRowNumber: form.dataStartRowNumber,
  charset: form.charset.trim() || 'UTF-8',
  delimiter: form.delimiter || ',',
  activeFlag: form.activeFlag,
  columns: form.columns
    .slice()
    .sort((a, b) => a.orderNo - b.orderNo)
    .map((column) => ({
      id: column.id > 0 ? column.id : null,
      columnName: column.columnName.trim(),
      csvHeaderName: column.csvHeaderName.trim(),
      dataType: column.dataType,
      requiredFlag: column.requiredFlag,
      keyFlag: column.keyFlag,
      nullableFlag: column.nullableFlag,
      trimFlag: column.trimFlag,
      defaultValue: nullable(column.defaultValue),
      formatPattern: nullable(column.formatPattern),
      updatableFlag: column.updatableFlag,
      orderNo: column.orderNo,
    })),
})