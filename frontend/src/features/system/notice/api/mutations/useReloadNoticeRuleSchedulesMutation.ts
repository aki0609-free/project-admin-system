import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'

export const useReloadNoticeRuleSchedulesMutation = () => {
  return useAppMutation({
    mutationFn: async (ruleId?: number) => {
      const path = ruleId
        ? `/api/system/notice-rules/schedules/${ruleId}/reload`
        : '/api/system/notice-rules/schedules/reload'

      return await post<void, undefined>(path, undefined)
    },
  })
}