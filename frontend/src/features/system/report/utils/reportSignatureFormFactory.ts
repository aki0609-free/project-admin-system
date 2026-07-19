import type {
  ReportSignatureResponse,
  ReportSignatureSaveRequest,
} from '@/features/system/report/types/reportSignatureApiTypes'
import type { ReportSignatureFormModel } from '@/features/system/report/types/reportSignatureFormTypes'
import { normalizeNullableString } from '@/shared/utils/StringUtils'

export const createEmptyReportSignatureForm =
  (): ReportSignatureFormModel => ({
    reportMasterId: 1,
    signatureName: '',
    signatureType: 'STAMP',
    fileName: '',
    contentType: 'image/png',
    width: 120,
    height: 60,
    displayOrder: 1,
    defaultFlag: false,
    activeFlag: true,
    signatureImageBase64: '',
  })

export const applyReportSignatureDetailToForm = (
  formModel: ReportSignatureFormModel,
  item: ReportSignatureResponse,
) => {
  formModel.reportMasterId = item.reportMasterId
  formModel.signatureName = item.signatureName
  formModel.signatureType = item.signatureType
  formModel.fileName = item.fileName ?? ''
  formModel.contentType = item.contentType ?? 'image/png'
  formModel.width = item.width ?? 120
  formModel.height = item.height ?? 60
  formModel.displayOrder = item.displayOrder ?? 1
  formModel.defaultFlag = item.defaultFlag
  formModel.activeFlag = item.activeFlag
  formModel.signatureImageBase64 = item.signatureImageBase64 ?? ''
}

export const toReportSignatureSaveRequest = (
  formModel: ReportSignatureFormModel,
): ReportSignatureSaveRequest => ({
  reportMasterId: formModel.reportMasterId,
  signatureType: formModel.signatureType,
  signatureName: formModel.signatureName.trim(),
  fileName: normalizeNullableString(formModel.fileName),
  contentType: normalizeNullableString(formModel.contentType),
  signatureImageBase64: normalizeNullableString(
    formModel.signatureImageBase64,
  ),
  width: formModel.width,
  height: formModel.height,
  displayOrder: formModel.displayOrder,
  defaultFlag: formModel.defaultFlag,
  activeFlag: formModel.activeFlag,
})