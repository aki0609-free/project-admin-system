import type { Ref } from 'vue'
import { fileToBase64 } from '@/shared/utils/FileUtils'
import type { ReportSignatureFormModel } from '@/features/system/report/types/reportSignatureFormTypes'

export const useReportSignatureFileUpload = (
  formModel: ReportSignatureFormModel,
  previewSrc: Ref<string>,
) => {
  const onFileChange = async (
    event: Event,
  ) => {
    const input = event.target as HTMLInputElement
    const file = input.files?.[0]

    if (!file) return

    formModel.fileName = file.name
    formModel.contentType = file.type || 'image/png'

    const base64 = await fileToBase64(file)

    formModel.signatureImageBase64 = base64
    previewSrc.value = base64
  }

  return {
    onFileChange,
  }
}