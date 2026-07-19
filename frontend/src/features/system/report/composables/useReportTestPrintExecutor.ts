/* eslint-disable @typescript-eslint/no-explicit-any */
import {
  onBeforeUnmount,
  ref,
  type Ref,
} from 'vue'

import { useTestPrintReportMutation }
  from '@/features/system/report/api/mutations/useTestPrintReportMutation'

export const useReportTestPrintExecutor = () => {
  const testPrintMutation =
    useTestPrintReportMutation()

  const previewVisible = ref(false)

  const pdfUrl =
    ref<string | null>(null)

  const clearPdfUrl = () => {
    if (!pdfUrl.value) return

    URL.revokeObjectURL(
      pdfUrl.value,
    )

    pdfUrl.value = null
  }

  onBeforeUnmount(() => {
    clearPdfUrl()
  })

  const downloadBlob = (
    blob: Blob,
    fileName: string,
  ) => {
    const url =
      URL.createObjectURL(blob)

    const a =
      document.createElement('a')

    a.href = url
    a.download = fileName
    a.click()

    URL.revokeObjectURL(url)
  }

  const execute = async (
    reportCode: string,
    params: Record<string, unknown>,
  ) => {
    const result =
      await testPrintMutation.mutateAsync({
        reportCode,
        params,
      }) as any

    if (
      result.contentType ===
      'application/pdf'
    ) {
      clearPdfUrl()

      pdfUrl.value =
        URL.createObjectURL(
          result.blob,
        )

      previewVisible.value = true

      return
    }

    downloadBlob(
      result.blob,
      result.fileName ?? 'download',
    )
  }

  return {
    previewVisible,
    pdfUrl,
    execute,
    isPending:
      testPrintMutation.isPending,
  }
}