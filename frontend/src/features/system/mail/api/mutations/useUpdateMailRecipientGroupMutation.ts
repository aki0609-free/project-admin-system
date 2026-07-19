import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'
import type {
  MailRecipientGroupResponse,
  MailRecipientGroupSaveRequest,
} from '@/features/system/mail/types/mailApiTypes'

type Payload = {
  id: number
  request: MailRecipientGroupSaveRequest
}

export const useUpdateMailRecipientGroupMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id, request }: Payload) =>
      await put<MailRecipientGroupResponse, MailRecipientGroupSaveRequest>(
        `/api/system/mail-recipient-groups/${id}`,
        request,
      ),

    onSuccess: async (_data: unknown, variables: Payload) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.mail.recipientGroups.all,
      })

      await queryClient.invalidateQueries({
        queryKey: queryKeys.mail.recipientGroups.detail(variables.id),
      })
    },
  })
}