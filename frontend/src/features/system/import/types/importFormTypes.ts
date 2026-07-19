import type {
  ImportDataType,
  ImportMode,
  ImportScriptType,
  ImportSourceType,
} from '@/features/system/import/types/importApiTypes'

export type ImportColumnForm = {
  id: number
  columnName: string
  csvHeaderName: string
  dataType: ImportDataType
  requiredFlag: boolean
  keyFlag: boolean
  nullableFlag: boolean
  trimFlag: boolean
  defaultValue: string
  formatPattern: string
  updatableFlag: boolean
  orderNo: number
}

export type ImportTargetDialogForm = {
  id: number
  targetCode: string
  targetName: string
  tableName: string
  description: string
  sourceType: ImportSourceType
  fixedFilePath: string
  scriptType: ImportScriptType
  scriptPath: string
  scriptArgs: string
  importMode: ImportMode
  headerRowNumber: number
  dataStartRowNumber: number
  charset: string
  delimiter: string
  activeFlag: boolean
  columns: ImportColumnForm[]
}