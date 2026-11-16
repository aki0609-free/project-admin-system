import { get, post, put } from '@/shared/api/http'
import type {
  StandardRemunerationConfirmRequest,
  StandardRemunerationDraftSaveRequest,
  StandardRemunerationEditorResponse,
} from '@/features/master/deduction/types/standardRemunerationEditorTypes'

const basePath = '/api/master/deductions/standard-remunerations'

export const standardRemunerationEditorApi = {
  find() {
    return get<StandardRemunerationEditorResponse>(basePath)
  },
  saveDraft(body: StandardRemunerationDraftSaveRequest) {
    return put<StandardRemunerationEditorResponse, StandardRemunerationDraftSaveRequest>(
      `${basePath}/draft`, body,
    )
  },
  confirm(batchId: number, body: StandardRemunerationConfirmRequest) {
    return post<StandardRemunerationEditorResponse, StandardRemunerationConfirmRequest>(
      `${basePath}/{batchId}/confirm`, body, { params: { path: { batchId } } },
    )
  },
}
