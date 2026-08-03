import { put } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'
import type {
  SpreadsheetLedgerSaveRequest,
  SpreadsheetLedgerSaveResponse,
} from '../types/operationBookTypes'

export function useSaveGeneratedSpreadsheetLedgerMutation() {
  return useAppMutation({
    mutationFn: (payload: {
      bookCode: string
      targetMonth: string
      selectionValue?: string | null
      request: SpreadsheetLedgerSaveRequest
    }) =>
      put<SpreadsheetLedgerSaveResponse, SpreadsheetLedgerSaveRequest>(
        `/api/operation/excel-books/${payload.bookCode}`
          + `/generated/${payload.targetMonth}`
          + (payload.selectionValue
            ? `/selections/${encodeURIComponent(payload.selectionValue)}`
            : ''),
        payload.request,
      ),
  })
}
