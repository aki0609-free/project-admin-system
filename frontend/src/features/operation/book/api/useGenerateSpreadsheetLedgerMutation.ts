import { post } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'
import type {
  SpreadsheetLedgerGenerateRequest,
  SpreadsheetLedgerGenerateResponse,
} from '../types/operationBookTypes'

export function useGenerateSpreadsheetLedgerMutation() {
  return useAppMutation({
    mutationFn: (payload: {
      bookCode: string
      request: SpreadsheetLedgerGenerateRequest
    }) =>
      post<
        SpreadsheetLedgerGenerateResponse,
        SpreadsheetLedgerGenerateRequest
      >(
        `/api/operation/excel-books/${payload.bookCode}/generate`,
        payload.request,
      ),
  })
}
