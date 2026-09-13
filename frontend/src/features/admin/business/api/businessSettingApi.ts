import { del, get, post, postForm, put } from '@/shared/api/http'
import type {
  AnnualReportBackupResult,
  AnnualReportBackupSetting,
  BusinessClosingSetting,
  ExternalSupportLinkSetting,
  MonthlyClosingOutputSaveRequest,
  MonthlyClosingOutputSetting,
  PayrollPolicySetting,
  PreviewReportSetting,
  PreviewReportSettingSaveRequest,
  ResignationChecklistItem,
  ResignationChecklistSaveRequest,
  ResignationMessage,
} from '../types/businessSettingTypes'

const basePath = '/api/admin/business-settings'

export const getResignationMessage = () =>
  get<ResignationMessage>(`${basePath}/resignation-message`)

export const saveResignationMessage = (request: ResignationMessage) =>
  put<ResignationMessage, ResignationMessage>(
    `${basePath}/resignation-message`,
    request,
  )

export const getResignationChecklist = () =>
  get<ResignationChecklistItem[]>(`${basePath}/resignation-checklist`)

export const createResignationChecklist = (
  request: ResignationChecklistSaveRequest,
) => post<ResignationChecklistItem, ResignationChecklistSaveRequest>(
  `${basePath}/resignation-checklist`,
  request,
)

export const updateResignationChecklist = (
  id: number,
  request: ResignationChecklistSaveRequest,
) => put<ResignationChecklistItem, ResignationChecklistSaveRequest>(
  `${basePath}/resignation-checklist/${id}`,
  request,
)

export const deleteResignationChecklist = (id: number) =>
  del<undefined>(`${basePath}/resignation-checklist/${id}`)

export const getClosingSetting = () =>
  get<BusinessClosingSetting>(`${basePath}/closing-setting`)

export const saveClosingSetting = (request: Pick<BusinessClosingSetting, 'closingDay' | 'paymentDay'>) =>
  put<BusinessClosingSetting, Pick<BusinessClosingSetting, 'closingDay' | 'paymentDay'>>(
    `${basePath}/closing-setting`,
    request,
  )

export const getClosingOutputs = () =>
  get<MonthlyClosingOutputSetting[]>(`${basePath}/closing-outputs`)

export const saveClosingOutputs = (requests: MonthlyClosingOutputSaveRequest[]) =>
  put<MonthlyClosingOutputSetting[], MonthlyClosingOutputSaveRequest[]>(
    `${basePath}/closing-outputs`,
    requests,
  )

export const getAnnualReportBackupSetting = () =>
  get<AnnualReportBackupSetting>(`${basePath}/annual-report-backup`)

export const saveAnnualReportBackupSetting = (request: AnnualReportBackupSetting) =>
  put<AnnualReportBackupSetting, AnnualReportBackupSetting>(
    `${basePath}/annual-report-backup`,
    request,
  )

export const executeAnnualReportBackup = (fiscalYear: number) =>
  post<AnnualReportBackupResult>(
    `${basePath}/annual-report-backup/${fiscalYear}/execute`,
  )

export const getExternalSupportLinkSetting = () =>
  get<ExternalSupportLinkSetting>(`${basePath}/external-support-links`)

export const saveExternalSupportLinkSetting = (
  request: ExternalSupportLinkSetting,
) => put<ExternalSupportLinkSetting, ExternalSupportLinkSetting>(
  `${basePath}/external-support-links`,
  request,
)

export const getPayrollPolicies = () =>
  get<PayrollPolicySetting[]>(`${basePath}/payroll-policies`)

export const savePayrollPolicy = (request: PayrollPolicySetting) =>
  post<PayrollPolicySetting, PayrollPolicySetting>(
    `${basePath}/payroll-policies`,
    request,
  )

export const deletePayrollPolicy = (id: number) =>
  del<undefined>(`${basePath}/payroll-policies/${id}`)

export const getPreviewReportSettings = () =>
  get<PreviewReportSetting[]>(`${basePath}/preview-reports`)

export const savePreviewReportSetting = (
  request: PreviewReportSettingSaveRequest,
  template: File | null,
) => {
  const formData = new FormData()
  formData.append(
    'definition',
    new Blob([JSON.stringify(request)], { type: 'application/json' }),
  )
  if (template) formData.append('template', template)
  return postForm<PreviewReportSetting>(`${basePath}/preview-reports`, formData)
}
