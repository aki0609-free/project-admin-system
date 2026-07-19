import { useQueryClient } from '@tanstack/vue-query'
import { put } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { queryKeys } from './queryKeys'
import type {
  DailyPreparationAssignmentResponse,
  DailyPreparationAssignmentSaveRequest,
} from '../types/dailyPreparationApiTypes'

type Payload = {
  id: number
  request: DailyPreparationAssignmentSaveRequest
}

export const useUpdateDailyPreparationAssignmentMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id, request }: Payload) =>
      await put<
        DailyPreparationAssignmentResponse,
        DailyPreparationAssignmentSaveRequest
      >(`/api/operation/daily-preparations/assignments/${id}`, request),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.dailyPreparations.all,
      })
    },
  })
}