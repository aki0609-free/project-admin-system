/* eslint-disable @typescript-eslint/no-explicit-any */
import { computed, onBeforeUnmount, ref } from 'vue'
import { useReportMastersQuery } from '@/features/system/report/api/queries/useReportMastersQuery'
import { useTestPrintReportMutation } from '@/features/system/report/api/mutations/useTestPrintReportMutation'
import { useReportTestPrintJson } from '@/features/system/report/composables/useReportTestPrintJson'

export const useReportTestPrint = () => {
  const { reportMasters } = useReportMastersQuery()

  const testPrintMutation =
    useTestPrintReportMutation()

  const selectedReportId =
    ref<number | null>(null)

  const selectedReport = computed(
    () =>
      reportMasters.value.find(
        x => x.id === selectedReportId.value,
      ) ?? null,
  )

  const selectedReportCode = computed(
    () =>
      selectedReport.value?.reportCode ??
      null,
  )

  const json = useReportTestPrintJson(
    selectedReportCode,
  )

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

  onBeforeUnmount(
    clearPdfUrl,
  )

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

  const executeTestPrint =
    async () => {
      if (!selectedReport.value) {
        alert(
          '帳票を選択してください。',
        )
        return
      }

      if (
        json.hasJsonError.value
      ) {
        alert(
          'JSON形式が不正です。',
        )
        return
      }

      const result =
        await testPrintMutation.mutateAsync(
          {
            reportCode:
              selectedReport.value
                .reportCode,
            params:
              json.apiParams.value,
          },
        ) as any

      if (
        result.contentType ===
        'application/pdf'
      ) {
        clearPdfUrl()

        pdfUrl.value =
          URL.createObjectURL(
            result.blob,
          )

        previewVisible.value =
          true

        return
      }

      downloadBlob(
        result.blob,
        result.fileName ??
          'download',
      )
    }

  return {
    reportMasters,

    selectedReportId,
    selectedReport,
    selectedReportCode,

    previewVisible,
    pdfUrl,

    testPrintMutation,

    executeTestPrint,

    ...json,
  }
}