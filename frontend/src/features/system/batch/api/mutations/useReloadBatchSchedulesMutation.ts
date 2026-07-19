import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'

export const useReloadBatchSchedulesMutation = () => {
  return useAppMutation({
    mutationFn: async (id?: number) => {
      const path = id
        ? `/api/system/batch/schedules/${id}/reload`
        : '/api/system/batch/schedules/reload'

      return await post<void, undefined>(path, undefined)
    },
  })
}