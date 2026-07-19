// src/features/application/api/applicationMediaBulkApi.ts
import { post } from '@/shared/api/http'
import type { ApplicationMediaBulkSaveRequest } from '@/features/application/types/types'

export const postApplicationMediaBulkSave = (
  payload: ApplicationMediaBulkSaveRequest,
) => {
  return post<void, ApplicationMediaBulkSaveRequest>(
    '/api/application-media/bulk-save',
    payload,
  )
}