import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/system/notice/api/queryKeys'
import type {
  NoticeRuleResponse,
  NoticeRuleSaveRequest,
} from '@/features/system/notice/types/noticeRuleApiTypes'

type Payload = {
  id: number
  request: NoticeRuleSaveRequest
}

export const useUpdateNoticeRuleMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id, request }: Payload) =>
      await put<NoticeRuleResponse, NoticeRuleSaveRequest>(
        `/api/system/notice-rules/${id}`,
        request,
      ),

    onSuccess: async (_data: unknown, variables: Payload) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.noticeRules.all,
      })

      await queryClient.invalidateQueries({
        queryKey: queryKeys.noticeRules.detail(variables.id),
      })
    },
  })
}