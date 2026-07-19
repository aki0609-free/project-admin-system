import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'
import type {
  MailSendResult,
  MailTestSendRequest,
} from '@/features/system/mail/types/mailApiTypes'

export const useSendTestMailMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: MailTestSendRequest) =>
      await post<MailSendResult, MailTestSendRequest>(
        '/api/system/mail/test-send',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.mail.queues.all,
      })
    },
  })
}