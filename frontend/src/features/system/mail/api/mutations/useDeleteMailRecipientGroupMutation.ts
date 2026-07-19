import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'

type Payload = {
  id: number
}

export const useDeleteMailRecipientGroupMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id }: Payload) =>
      await del<void>(`/api/system/mail-recipient-groups/${id}`),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.mail.recipientGroups.all,
      })
    },
  })
}