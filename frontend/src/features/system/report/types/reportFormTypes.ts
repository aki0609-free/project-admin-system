import type {
  ReportCleanupType,
  ReportLayoutType,
  ReportOutputFormat,
  ReportParamControlType,
  ReportParamType,
  ReportPreProcessType,
} from './reportEnums'

export type ReportParamBase = {
  paramName: string
  paramLabel: string
  paramType: ReportParamType
  controlType: ReportParamControlType
  requiredFlag: boolean
  visibleFlag: boolean
  multipleFlag: boolean
  filterFlag: boolean
  defaultValue?: string | null
  placeholder?: string | null
  inputColumnName?: string | null
  displayOrder: number
  activeFlag: boolean
}

export type ReportParamFormItem = ReportParamBase & {
  id: number
}

export type ReportMasterForm = {
  reportCode: string
  reportName: string
  templateFileName: string | null
  workTable: string
  inputTable: string | null
  outputTable: string | null
  preProcessType: ReportPreProcessType
  preProcessSql: string | null
  procedureName: string | null
  querySql: string
  cleanupType: ReportCleanupType
  cleanupSql: string | null
  cleanupProcedureName: string | null
  layoutType: ReportLayoutType
  layoutCount: number | null
  fileName: string | null
  outputFormat: ReportOutputFormat
  useSignature: boolean
  previewEnabled: boolean
  activeFlag: boolean
  params: ReportParamFormItem[]
}