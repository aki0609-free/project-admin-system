import { del, get, post, put } from '@/shared/api/http'
import type {
  AnnualReportBackupResult,
  AnnualReportBackupSetting,
  BusinessClosingSetting,
  DormitoryFeeSetting,
  DormitoryFeeSettingSaveRequest,
  ExternalSupportLinkSetting,
  MonthlyClosingOutputSaveRequest,
  MonthlyClosingOutputSetting,
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

export const getDormitoryFees = () =>
  get<DormitoryFeeSetting[]>(`${basePath}/dormitory-fees`)

export const saveDormitoryFees = (requests: DormitoryFeeSettingSaveRequest[]) =>
  put<DormitoryFeeSetting[], DormitoryFeeSettingSaveRequest[]>(
    `${basePath}/dormitory-fees`,
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
