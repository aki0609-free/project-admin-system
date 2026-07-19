import type { OperationType } from '../types/operationReportPreviewTypes'

export const operationReportPreviewQueryKeys = {
  all: ['operation-report-previews'] as const,

  byOperationType: (operationType: OperationType) =>
    ['operation-report-previews', operationType] as const,
}