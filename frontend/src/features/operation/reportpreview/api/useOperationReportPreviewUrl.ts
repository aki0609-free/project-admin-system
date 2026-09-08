import { computed, type Ref } from 'vue'
import type {
  OperationReportPreviewResponse,
  OperationType,
} from '../types/operationReportPreviewTypes'

export const useOperationReportPreviewUrl = ({
  operationType,
  selectedReport,
  targetDate,
  targetMonth,
  customerId,
  periodFrom,
  periodTo,
}: {
  operationType: Ref<OperationType>
  selectedReport: Ref<OperationReportPreviewResponse | null>
  targetDate?: Ref<string | null | undefined>
  targetMonth?: Ref<string | null | undefined>
  customerId?: Ref<number | null | undefined>
  periodFrom?: Ref<string | null | undefined>
  periodTo?: Ref<string | null | undefined>
}) => {
  const previewUrl = computed(() => {
    if (!selectedReport.value) return ''

    const params = new URLSearchParams()

    params.set('operationType', operationType.value)
    params.set('reportCode', selectedReport.value.reportCode)

    if (targetDate?.value) {
      params.set('targetDate', targetDate.value)
    }

    if (targetMonth?.value) {
      params.set('targetMonth', targetMonth.value)
    }

    if (customerId?.value) {
      params.set('customerId', String(customerId.value))
    }

    if (periodFrom?.value) {
      params.set('periodFrom', periodFrom.value)
    }

    if (periodTo?.value) {
      params.set('periodTo', periodTo.value)
    }

    return `/api/operation/report-previews/html?${params.toString()}`
  })

  return {
    previewUrl,
  }
}
