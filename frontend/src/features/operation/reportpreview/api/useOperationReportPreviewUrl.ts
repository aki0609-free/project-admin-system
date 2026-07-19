import { computed, type Ref } from 'vue'
import type {
  OperationReportPreviewResponse,
  OperationType,
} from '../types/operationReportPreviewTypes'

const API_BASE_URL = 'http://localhost:8080'

export const useOperationReportPreviewUrl = ({
  operationType,
  selectedReport,
  targetDate,
  targetMonth,
}: {
  operationType: Ref<OperationType>
  selectedReport: Ref<OperationReportPreviewResponse | null>
  targetDate?: Ref<string | null | undefined>
  targetMonth?: Ref<string | null | undefined>
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

    return `${API_BASE_URL}/api/operation/report-previews/html?${params.toString()}`
  })

  return {
    previewUrl,
  }
}