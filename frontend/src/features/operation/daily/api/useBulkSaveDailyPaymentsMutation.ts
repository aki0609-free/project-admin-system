import { useQueryClient } from '@tanstack/vue-query'
import { post } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'
import type {
  DailyPaymentBulkSaveRequest,
  DailyPaymentResponse,
} from '../types/dailyPaymentApiTypes'
import { dailyPaymentQueryKeys } from './queryKeys'

export const useBulkSaveDailyPaymentsMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: DailyPaymentBulkSaveRequest) =>
      await post<DailyPaymentResponse[], DailyPaymentBulkSaveRequest>(
        '/api/operation/daily-payments/bulk-save',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: dailyPaymentQueryKeys.all,
      })
    },
  })
}