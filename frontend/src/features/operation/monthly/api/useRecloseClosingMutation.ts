/* eslint-disable @typescript-eslint/no-invalid-void-type */
import { useQueryClient } from '@tanstack/vue-query'
import { post } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'
import type { MonthlyClosingResponse } from '../types/closingApiTypes'
import { queryKeys } from './closingQueryKeys'

export const useRecloseClosingMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: (targetMonth: string) =>
      post<MonthlyClosingResponse, void>(
        '/api/operation/monthly/reclose',
        undefined,
        {
          params: {
            query: {
              targetMonth,
            },
          },
        },
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.closings.all,
      })
    },
  })
}