import { useQueryClient } from '@tanstack/vue-query'
import { post } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { queryKeys } from './queryKeys'
import type {
  DailyPreparationDispatchBulkSaveRequest,
  DailyPreparationResponse,
} from '../types/dailyPreparationApiTypes'

export const useBulkSaveDailyPreparationDispatchesMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: DailyPreparationDispatchBulkSaveRequest) =>
      await post<DailyPreparationResponse, DailyPreparationDispatchBulkSaveRequest>(
        '/api/operation/daily-preparations/dispatches/bulk-save',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.dailyPreparations.all,
      })
    },
  })
}