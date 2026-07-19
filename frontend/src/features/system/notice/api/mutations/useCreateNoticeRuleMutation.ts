import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/notice/api/queryKeys'
import type {
  NoticeRuleResponse,
  NoticeRuleSaveRequest,
} from '@/features/system/notice/types/noticeRuleApiTypes'

export const useCreateNoticeRuleMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: NoticeRuleSaveRequest) =>
      await post<NoticeRuleResponse, NoticeRuleSaveRequest>(
        '/api/system/notice-rules',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.noticeRules.all,
      })
    },
  })
}