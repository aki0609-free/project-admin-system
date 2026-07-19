import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'
import type { MailRecipientGroupResponse } from '@/features/system/mail/types/mailApiTypes'

export const useMailRecipientGroupsQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.mail.recipientGroups.list(),
    queryFn: async () =>
      await get<MailRecipientGroupResponse[]>('/api/system/mail-recipient-groups'),
  })

  return {
    ...query,
    groups: computed(() => query.data.value ?? []),
  }
}