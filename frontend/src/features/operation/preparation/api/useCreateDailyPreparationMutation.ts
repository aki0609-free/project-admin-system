import { useQueryClient } from '@tanstack/vue-query'
import { post } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { queryKeys } from './queryKeys'
import type {
  DailyPreparationCreateRequest,
  DailyPreparationResponse,
} from '../types/dailyPreparationApiTypes'

export const useCreateDailyPreparationMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: DailyPreparationCreateRequest) =>
      await post<DailyPreparationResponse, DailyPreparationCreateRequest>(
        '/api/operation/daily-preparations',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.dailyPreparations.all,
      })
    },
  })
}