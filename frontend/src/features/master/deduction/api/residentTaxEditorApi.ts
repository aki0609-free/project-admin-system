import { get, post, put } from '@/shared/api/http'
import type {
  ResidentTaxConfirmRequest,
  ResidentTaxDraftSaveRequest,
  ResidentTaxEditorResponse,
} from '@/features/master/deduction/types/residentTaxEditorTypes'

export const residentTaxEditorApi = {
  find(fiscalYear: number) {
    return get<ResidentTaxEditorResponse>('/api/master/deductions/resident-tax', {
      params: { query: { fiscalYear } },
    })
  },

  saveDraft(body: ResidentTaxDraftSaveRequest) {
    return put<ResidentTaxEditorResponse, ResidentTaxDraftSaveRequest>(
      '/api/master/deductions/resident-tax/draft',
      body,
    )
  },

  confirm(batchId: number, body: ResidentTaxConfirmRequest) {
    return post<ResidentTaxEditorResponse, ResidentTaxConfirmRequest>(
      '/api/master/deductions/resident-tax/{batchId}/confirm',
      body,
      { params: { path: { batchId } } },
    )
  },
}
