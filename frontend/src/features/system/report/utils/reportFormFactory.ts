import type {
  ReportMasterDetailResponse,
  ReportMasterSaveRequest,
} from '@/features/system/report/types/reportMasterApiTypes'
import type { ReportMasterForm } from '@/features/system/report/types/reportFormTypes'
import {
  toReportParamFormItems,
  toReportParamSaveRequests,
} from '@/features/system/report/utils/reportParamFormFactory'
import { normalizeNullableString } from '@/shared/utils/StringUtils'

export const createEmptyReportMasterForm = (): ReportMasterForm => ({
  reportCode: '',
  reportName: '',
  templateFileName: null,
  workTable: '',
  inputTable: null,
  outputTable: null,
  preProcessType: 'NONE',
  preProcessSql: null,
  procedureName: null,
  querySql: '',
  cleanupType: 'NONE',
  cleanupSql: null,
  cleanupProcedureName: null,
  layoutType: 'SINGLE',
  layoutCount: 1,
  fileName: null,
  outputFormat: 'PDF',
  useSignature: false,
  previewEnabled: true,
  activeFlag: true,
  params: [],
})

export const toReportMasterForm = (
  detail: ReportMasterDetailResponse,
): ReportMasterForm => ({
  reportCode: detail.reportCode,
  reportName: detail.reportName,
  templateFileName: detail.templateFileName ?? null,
  workTable: detail.workTable,
  inputTable: detail.inputTable ?? null,
  outputTable: detail.outputTable ?? null,
  preProcessType: detail.preProcessType,
  preProcessSql: detail.preProcessSql ?? null,
  procedureName: detail.procedureName ?? null,
  querySql: detail.querySql ?? '',
  cleanupType: detail.cleanupType,
  cleanupSql: detail.cleanupSql ?? null,
  cleanupProcedureName: detail.cleanupProcedureName ?? null,
  layoutType: detail.layoutType,
  layoutCount: detail.layoutCount ?? 1,
  fileName: detail.fileName ?? null,
  outputFormat: detail.outputFormat,
  useSignature: detail.useSignature,
  previewEnabled: detail.previewEnabled,
  activeFlag: detail.activeFlag,
  params: toReportParamFormItems(detail.params),
})

export const toReportMasterSaveRequest = (
  form: ReportMasterForm,
): ReportMasterSaveRequest => ({
  reportCode: form.reportCode.trim(),
  reportName: form.reportName.trim(),
  templateFileName: normalizeNullableString(form.templateFileName),
  workTable: form.workTable.trim(),
  inputTable: normalizeNullableString(form.inputTable),
  outputTable: normalizeNullableString(form.outputTable),
  preProcessType: form.preProcessType,
  preProcessSql: normalizeNullableString(form.preProcessSql),
  procedureName: normalizeNullableString(form.procedureName),
  querySql: form.querySql.trim(),
  cleanupType: form.cleanupType,
  cleanupSql: normalizeNullableString(form.cleanupSql),
  cleanupProcedureName: normalizeNullableString(form.cleanupProcedureName),
  layoutType: form.layoutType,
  layoutCount: form.layoutCount,
  fileName: normalizeNullableString(form.fileName),
  outputFormat: form.outputFormat,
  useSignature: form.useSignature,
  previewEnabled: form.previewEnabled,
  activeFlag: form.activeFlag,
  params: toReportParamSaveRequests(form.params),
})