import type {
  ReportHistoryStatus,
  ReportOutputFormat,
  ReportStorageType,
} from './reportEnums'

export type ReportHistoryBase = {
  id: number
  reportMasterId: number
  reportCode?: string | null
  reportName?: string | null
  fileName?: string | null
  outputFormat: ReportOutputFormat
  status: ReportHistoryStatus
  executedBy?: string | null
  requestParamsJson?: string | null
  notes?: string | null
  storageType?: ReportStorageType | null
  storedFileKey?: string | null
  storedFileName?: string | null
  mimeType?: string | null
  fileSize?: number | null
  createdAt?: string | null
}

export type ReportHistoryResponse = ReportHistoryBase

export type ReportHistoryDetailResponse = ReportHistoryBase