/* eslint-disable @typescript-eslint/no-invalid-void-type */
import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from './queryKeys'

export const useDeleteExcelBookMasterMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (id: number) => {
      await del<void>(`/api/system/excel-book-masters/${id}`)
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.excelBookMasters.all,
      })
    },
  })
}