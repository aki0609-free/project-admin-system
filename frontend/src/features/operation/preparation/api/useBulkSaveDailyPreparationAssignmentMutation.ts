import { useQueryClient } from '@tanstack/vue-query'
import { post } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { queryKeys } from './queryKeys'
import type {
  DailyPreparationAssignmentBulkSaveRequest,
  DailyPreparationResponse,
} from '../types/dailyPreparationApiTypes'

export const useBulkSaveDailyPreparationAssignmentsMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: DailyPreparationAssignmentBulkSaveRequest) =>
      await post<DailyPreparationResponse, DailyPreparationAssignmentBulkSaveRequest>(
        '/api/operation/daily-preparations/assignments/bulk-save',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.dailyPreparations.all,
      })
    },
  })
}