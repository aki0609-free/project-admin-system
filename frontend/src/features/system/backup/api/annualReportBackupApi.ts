import { get, post, put } from '@/shared/api/http'
import type {
  AnnualReportBackupResult,
  AnnualReportBackupSetting,
} from '../types/annualReportBackupTypes'

const basePath = '/api/system/backup/annual-reports'

export const getAnnualReportBackupSetting = () =>
  get<AnnualReportBackupSetting>(`${basePath}/setting`)

export const saveAnnualReportBackupSetting = (request: AnnualReportBackupSetting) =>
  put<AnnualReportBackupSetting, AnnualReportBackupSetting>(
    `${basePath}/setting`,
    request,
  )

export const executeAnnualReportBackup = (fiscalYear: number) =>
  post<AnnualReportBackupResult>(`${basePath}/${fiscalYear}/execute`)
