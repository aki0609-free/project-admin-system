import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'
import type { MailSendResult } from '@/features/system/mail/types/mailApiTypes'

export const useSendWaitingMailsMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async () =>
      await post<MailSendResult, undefined>(
        '/api/system/mail/send-waiting',
        undefined,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.mail.queues.all,
      })
    },
  })
}