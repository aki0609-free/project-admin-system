import { useMutation } from '@tanstack/vue-query'
import { postBlob } from '@/shared/api/http'
import type { SpreadsheetLedgerSaveRequest } from '../types/operationBookTypes'

export function useExportSpreadsheetLedgerXlsxMutation() {
  return useMutation({
    mutationFn: ({
      bookCode,
      request,
    }: {
      bookCode: string
      request: SpreadsheetLedgerSaveRequest
    }) => postBlob(
      `/api/operation/excel-books/${encodeURIComponent(bookCode)}/export/xlsx`,
      request,
    ),
  })
}
