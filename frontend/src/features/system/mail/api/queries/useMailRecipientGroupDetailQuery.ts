import { computed, type ComputedRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'
import type { MailRecipientGroupResponse } from '@/features/system/mail/types/mailApiTypes'

export const useMailRecipientGroupDetailQuery = (
  id: ComputedRef<number | null>,
) => {
  const query = useAppQuery({
    queryKey: computed(() => queryKeys.mail.recipientGroups.detail(id.value)),
    enabled: computed(() => id.value != null),
    queryFn: async () =>
      await get<MailRecipientGroupResponse>(
        `/api/system/mail-recipient-groups/${id.value}`,
      ),
  })

  return {
    ...query,
    group: computed(() => query.data.value ?? null),
  }
}