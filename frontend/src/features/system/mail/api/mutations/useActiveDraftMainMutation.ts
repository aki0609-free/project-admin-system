import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'
import type { MailActivateDraftRequest } from '@/features/system/mail/types/mailApiTypes'

export const useActivateDraftMailMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: MailActivateDraftRequest) =>
      await post<void, MailActivateDraftRequest>(
        '/api/system/mail/activate-draft',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.mail.queues.all,
      })
    },
  })
}