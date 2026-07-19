import { useAppMutation } from '@/shared/api/useAppMutation'
import { postBlob } from '@/shared/api/http'
import type { ExcelBookUpdateRequest } from '../types/excelBookTypes'

export const useUpdateExcelBookMutation = () => {
  return useAppMutation({
    mutationFn: async (payload: {
      bookCode: string
      request: ExcelBookUpdateRequest
    }) => {
      return await postBlob<ExcelBookUpdateRequest>(
        `/api/system/excel-books/${payload.bookCode}/update`,
        payload.request,
      )
    },
  })
}