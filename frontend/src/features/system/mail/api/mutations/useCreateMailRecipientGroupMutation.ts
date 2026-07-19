import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'
import type {
  MailRecipientGroupResponse,
  MailRecipientGroupSaveRequest,
} from '@/features/system/mail/types/mailApiTypes'

export const useCreateMailRecipientGroupMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: MailRecipientGroupSaveRequest) =>
      await post<MailRecipientGroupResponse, MailRecipientGroupSaveRequest>(
        '/api/system/mail-recipient-groups',
        request,
      ),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.mail.recipientGroups.all,
      })
    },
  })
}