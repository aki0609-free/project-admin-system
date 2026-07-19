import type { ReportSignatureType } from './reportEnums'

export type ReportSignatureFormModel = {
  reportMasterId: number
  signatureName: string
  signatureType: ReportSignatureType
  fileName: string
  contentType: string
  width: number
  height: number
  displayOrder: number
  defaultFlag: boolean
  activeFlag: boolean
  signatureImageBase64: string
}