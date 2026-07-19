import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'
import type {
  MailPdfSendRequest,
  MailSendResult,
} from '@/features/system/mail/types/mailApiTypes'

export const useSendPdfMailMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: MailPdfSendRequest) =>
      await post<MailSendResult, MailPdfSendRequest>(
        '/api/system/mail/pdf/send',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.mail.queues.all,
      })
    },
  })
}