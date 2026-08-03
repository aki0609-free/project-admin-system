import { useQueryClient } from '@tanstack/vue-query'
import { put } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { queryKeys } from './queryKeys'
import type {
  SpreadsheetTemplateResponse,
  SpreadsheetTemplateSaveRequest,
} from '../types/excelBookTypes'

export const useSaveSpreadsheetTemplateMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (payload: {
      masterId: number
      request: SpreadsheetTemplateSaveRequest
    }) => {
      return await put<
        SpreadsheetTemplateResponse,
        SpreadsheetTemplateSaveRequest
      >(
        `/api/system/excel-book-masters/${payload.masterId}/spreadsheet-template`,
        payload.request,
      )
    },
    onSuccess: async (_: unknown, payload: { masterId: number }) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.spreadsheetTemplates.detail(payload.masterId),
      })
    },
  })
}
