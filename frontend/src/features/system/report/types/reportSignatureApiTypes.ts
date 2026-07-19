import type { ReportSignatureType } from './reportEnums'

export type ReportSignatureResponse = {
  id: number
  reportMasterId: number
  signatureType: ReportSignatureType
  signatureName: string
  fileName?: string | null
  contentType?: string | null
  width?: number | null
  height?: number | null
  displayOrder: number
  defaultFlag: boolean
  activeFlag: boolean
  signatureImageBase64?: string | null
}

export type ReportSignatureSaveRequest = {
  reportMasterId: number
  signatureType: ReportSignatureType
  signatureName: string
  fileName?: string | null
  contentType?: string | null
  width?: number | null
  height?: number | null
  displayOrder: number
  defaultFlag: boolean
  activeFlag: boolean
  signatureImageBase64?: string | null
}

export type ReportSignatureSaveResponse = {
  id: number
  message: string
}