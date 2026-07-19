import { computed, ref, type Ref } from 'vue'
import type { CustomerListItem } from '@/features/master/customer/types/customerTypes'
import type {
  EnvelopePrintCustomerOption,
  EnvelopePrintPayload,
} from '@/features/master/customer/types/envelopePrintTypes'
import type {
  BatchExecuteResponse,
  StorageType,
} from '@/features/system/batch/types/batchApiTypes'
import { useExecuteBatchMutation } from '@/features/system/batch/api/mutations/useExecuteBatchMutation'
import { useDownloadBatchLogFileMutation } from '@/features/system/batch/api/mutations/useDownloadBatchLogFileMutation'

const toEnvelopeJobCode = (
  envelopeType: EnvelopePrintPayload['envelopeType'],
) => {
  return envelopeType === 'NAGA3'
    ? 'PRINT_ENVELOPE_NAGA3'
    : 'PRINT_ENVELOPE_KAKU2'
}

export const useEnvelopePrintDialog = (
  customers: Ref<CustomerListItem[]>,
) => {
  const envelopeDialog = ref(false)

  const pdfPreviewDialog = ref(false)
  const pdfPreviewUrl = ref<string | null>(null)
  const pdfPreviewTitle = ref('封筒印刷プレビュー')

  const pdfFileKey = ref<string | null>(null)
  const pdfFileName = ref<string | null>(null)
  const pdfStorageType = ref<StorageType | null>(null)

  const executeBatchMutation = useExecuteBatchMutation()
  const downloadBatchLogFileMutation = useDownloadBatchLogFileMutation()

  const isPrinting = computed(
    () =>
      executeBatchMutation.isPending.value ||
      downloadBatchLogFileMutation.isPending.value,
  )

  const envelopeCustomerOptions = computed<EnvelopePrintCustomerOption[]>(() =>
    customers.value.map(customer => ({
      id: customer.id,
      name: customer.name,
      address: customer.address,
    })),
  )

  const openEnvelopePrint = () => {
    envelopeDialog.value = true
  }

  const clearPdfFileInfo = () => {
    pdfFileKey.value = null
    pdfFileName.value = null
    pdfStorageType.value = null
  }

  const revokePdfPreviewUrl = () => {
    if (!pdfPreviewUrl.value) return

    URL.revokeObjectURL(pdfPreviewUrl.value)
    pdfPreviewUrl.value = null
  }

  const closePdfPreview = () => {
    pdfPreviewDialog.value = false
    revokePdfPreviewUrl()
    clearPdfFileInfo()
  }

  const handleEnvelopePrint = async (
    payload: EnvelopePrintPayload,
  ) => {
    const jobCode = toEnvelopeJobCode(payload.envelopeType)

    const result: BatchExecuteResponse =
      await executeBatchMutation.mutateAsync({
        jobCode,
        params: {
          customerIds: payload.customerIds,
          envelopeType: payload.envelopeType,
          stamp: payload.stamp,
          honorific: payload.honorific,
          fontFamily: payload.fontFamily,
          fontSize: payload.fontSize,
        },
      }) as BatchExecuteResponse

    if (result.status !== 'COMPLETED' || !result.logId) {
      alert(result.message)
      return
    }

    const blob = await downloadBatchLogFileMutation.mutateAsync(result.logId) as Blob

    revokePdfPreviewUrl()

    pdfPreviewUrl.value = URL.createObjectURL(blob)
    pdfPreviewTitle.value = result.outputFileName ?? `${jobCode}.pdf`

    pdfFileKey.value = result.outputFileKey ?? null
    pdfFileName.value = result.outputFileName ?? `${jobCode}.pdf`
    pdfStorageType.value = result.storageType ?? 'LOCAL'

    pdfPreviewDialog.value = true
  }

  return {
    envelopeDialog,
    envelopeCustomerOptions,

    pdfPreviewDialog,
    pdfPreviewUrl,
    pdfPreviewTitle,
    pdfFileKey,
    pdfFileName,
    pdfStorageType,

    openEnvelopePrint,
    closePdfPreview,
    handleEnvelopePrint,

    isPrinting,
  }
}