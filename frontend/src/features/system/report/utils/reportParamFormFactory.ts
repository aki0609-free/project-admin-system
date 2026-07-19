import type {
  ReportParamRequest,
  ReportParamResponse,
} from '@/features/system/report/types/reportMasterApiTypes'
import type { ReportParamFormItem } from '@/features/system/report/types/reportFormTypes'
import { normalizeNullableString } from '@/shared/utils/StringUtils'

let tempIdSeq = -1

const nextTempId = () => tempIdSeq--

export const createEmptyReportParamFormItem = (
  displayOrder = 1,
): ReportParamFormItem => ({
  id: nextTempId(),
  paramName: '',
  paramLabel: '',
  paramType: 'STRING',
  controlType: 'TEXT',
  requiredFlag: false,
  visibleFlag: true,
  multipleFlag: false,
  filterFlag: true,
  defaultValue: null,
  placeholder: null,
  inputColumnName: null,
  displayOrder,
  activeFlag: true,
})

export const toReportParamFormItem = (
  param: ReportParamResponse,
): ReportParamFormItem => ({
  id: param.id,
  paramName: param.paramName,
  paramLabel: param.paramLabel,
  paramType: param.paramType,
  controlType: param.controlType,
  requiredFlag: param.requiredFlag,
  visibleFlag: param.visibleFlag,
  multipleFlag: param.multipleFlag,
  filterFlag: param.filterFlag,
  defaultValue: param.defaultValue ?? null,
  placeholder: param.placeholder ?? null,
  inputColumnName: param.inputColumnName ?? null,
  displayOrder: param.displayOrder,
  activeFlag: param.activeFlag,
})

export const toReportParamFormItems = (
  params: ReportParamResponse[] | null | undefined,
): ReportParamFormItem[] =>
  (params ?? []).map(toReportParamFormItem)

export const toReportParamSaveRequest = (
  param: ReportParamFormItem,
): ReportParamRequest => ({
  id: param.id > 0 ? param.id : null,
  paramName: param.paramName.trim(),
  paramLabel: param.paramLabel.trim(),
  paramType: param.paramType,
  controlType: param.controlType,
  requiredFlag: param.requiredFlag,
  visibleFlag: param.visibleFlag,
  multipleFlag: param.multipleFlag,
  filterFlag: param.filterFlag,
  defaultValue: normalizeNullableString(param.defaultValue),
  placeholder: normalizeNullableString(param.placeholder),
  inputColumnName: normalizeNullableString(param.inputColumnName),
  displayOrder: param.displayOrder,
  activeFlag: param.activeFlag,
})

export const toReportParamSaveRequests = (
  params: ReportParamFormItem[],
): ReportParamRequest[] =>
  [...params]
    .sort((a, b) => a.displayOrder - b.displayOrder)
    .map(toReportParamSaveRequest)

export const resequenceReportParams = (
  params: ReportParamFormItem[],
): ReportParamFormItem[] =>
  params.map((param, index) => ({
    ...param,
    displayOrder: index + 1,
  }))