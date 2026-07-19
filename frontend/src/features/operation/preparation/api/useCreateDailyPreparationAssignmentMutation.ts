import { useQueryClient } from '@tanstack/vue-query'
import { post } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { queryKeys } from './queryKeys'
import type {
  DailyPreparationAssignmentResponse,
  DailyPreparationAssignmentSaveRequest,
} from '../types/dailyPreparationApiTypes'

export const useCreateDailyPreparationAssignmentMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: DailyPreparationAssignmentSaveRequest) =>
      await post<
        DailyPreparationAssignmentResponse,
        DailyPreparationAssignmentSaveRequest
      >('/api/operation/daily-preparations/assignments', request),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.dailyPreparations.all,
      })
    },
  })
}