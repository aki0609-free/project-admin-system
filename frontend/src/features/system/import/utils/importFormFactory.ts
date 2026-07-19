import type { ImportTargetResponse } from '@/features/system/import/types/importApiTypes'
import type {
  ImportColumnForm,
  ImportTargetDialogForm,
} from '@/features/system/import/types/importFormTypes'

let tempColumnId = -1

export const createEmptyImportColumn = (orderNo = 1): ImportColumnForm => ({
  id: tempColumnId--,
  columnName: '',
  csvHeaderName: '',
  dataType: 'STRING',
  requiredFlag: false,
  keyFlag: false,
  nullableFlag: true,
  trimFlag: true,
  defaultValue: '',
  formatPattern: '',
  updatableFlag: true,
  orderNo,
})

export const createEmptyImportTargetForm = (): ImportTargetDialogForm => ({
  id: 0,
  targetCode: '',
  targetName: '',
  tableName: '',
  description: '',
  sourceType: 'UPLOAD',
  fixedFilePath: '',
  scriptType: 'NONE',
  scriptPath: '',
  scriptArgs: '',
  importMode: 'INSERT_ONLY',
  headerRowNumber: 1,
  dataStartRowNumber: 2,
  charset: 'UTF-8',
  delimiter: ',',
  activeFlag: true,
  columns: [],
})

export const toImportTargetDialogForm = (
  target: ImportTargetResponse,
): ImportTargetDialogForm => ({
  id: target.id,
  targetCode: target.targetCode,
  targetName: target.targetName,
  tableName: target.tableName,
  description: target.description ?? '',
  sourceType: target.sourceType,
  fixedFilePath: target.fixedFilePath ?? '',
  scriptType: target.scriptType,
  scriptPath: target.scriptPath ?? '',
  scriptArgs: target.scriptArgs ?? '',
  importMode: target.importMode,
  headerRowNumber: target.headerRowNumber,
  dataStartRowNumber: target.dataStartRowNumber,
  charset: target.charset,
  delimiter: target.delimiter,
  activeFlag: target.activeFlag,
  columns: (target.columns ?? []).map((column) => ({
    id: column.id,
    columnName: column.columnName,
    csvHeaderName: column.csvHeaderName,
    dataType: column.dataType,
    requiredFlag: column.requiredFlag,
    keyFlag: column.keyFlag,
    nullableFlag: column.nullableFlag,
    trimFlag: column.trimFlag,
    defaultValue: column.defaultValue ?? '',
    formatPattern: column.formatPattern ?? '',
    updatableFlag: column.updatableFlag,
    orderNo: column.orderNo,
  })),
})