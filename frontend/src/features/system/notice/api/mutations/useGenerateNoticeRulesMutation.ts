import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys as dashboardQueryKeys } from '@/features/dashboard/api/queryKeys'
import { queryKeys } from '@/features/system/notice/api/queryKeys'
import type { NoticeGenerateResult } from '@/features/system/notice/types/noticeRuleApiTypes'

export const useGenerateNoticeRulesMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (ruleId?: number) => {
      const path = ruleId
        ? `/api/system/notice-rules/generate/${ruleId}`
        : '/api/system/notice-rules/generate'

      return await post<NoticeGenerateResult, undefined>(path, undefined)
    },

    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.noticeRules.all,
        }),
        queryClient.invalidateQueries({
          queryKey: dashboardQueryKeys.notice.all,
        }),
      ])
    },
  })
}
