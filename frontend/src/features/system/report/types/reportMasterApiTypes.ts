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

export type ReportParamRequest = ReportParamBase & {
  id?: number | null
}

export type ReportParamResponse = ReportParamBase & {
  id: number
  reportMasterId: number
}

export type ReportMasterBase = {
  reportCode: string
  reportName: string
  templateFileName?: string | null
  workTable: string
  inputTable?: string | null
  outputTable?: string | null
  preProcessType: ReportPreProcessType
  preProcessSql?: string | null
  procedureName?: string | null
  querySql: string
  cleanupType: ReportCleanupType
  cleanupSql?: string | null
  cleanupProcedureName?: string | null
  layoutType: ReportLayoutType
  layoutCount?: number | null
  fileName?: string | null
  outputFormat: ReportOutputFormat
  useSignature: boolean
  previewEnabled: boolean
  activeFlag: boolean
}

export type ReportMasterSaveRequest = ReportMasterBase & {
  params: ReportParamRequest[]
}

export type ReportMasterListItemResponse = Omit<
  ReportMasterBase,
  | 'querySql'
  | 'cleanupType'
  | 'cleanupSql'
  | 'cleanupProcedureName'
  | 'layoutType'
  | 'layoutCount'
  | 'fileName'
> & {
  id: number
}

export type ReportMasterDetailResponse = ReportMasterBase & {
  id: number
  params: ReportParamResponse[]
}