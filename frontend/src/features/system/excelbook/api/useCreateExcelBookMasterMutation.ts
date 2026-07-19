import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import type {
  ExcelBookMasterRequest,
} from '../types/excelBookTypes'

export const useCreateExcelBookMasterMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: ExcelBookMasterRequest) => {
      return await post<number, ExcelBookMasterRequest>(
        '/api/system/excel-book-masters',
        request,
      )
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.excelBookMasters.all,
      })
    },
  })
}