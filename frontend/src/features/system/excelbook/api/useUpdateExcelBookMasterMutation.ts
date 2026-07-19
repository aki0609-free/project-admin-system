/* eslint-disable @typescript-eslint/no-invalid-void-type */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import type { ExcelBookMasterRequest } from '../types/excelBookTypes'

export const useUpdateExcelBookMasterMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (payload: { id: number; request: ExcelBookMasterRequest }) => {
      await put<void, ExcelBookMasterRequest>(
        `/api/system/excel-book-masters/${payload.id}`,
        payload.request,
      )
    },
    onSuccess: async (_: unknown, payload: { id: number }) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.excelBookMasters.all,
      })
      await queryClient.invalidateQueries({
        queryKey: queryKeys.excelBookMasters.detail(payload.id),
      })
    },
  })
}