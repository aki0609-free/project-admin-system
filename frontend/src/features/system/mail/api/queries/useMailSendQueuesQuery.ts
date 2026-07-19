import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'
import type { MailSendQueueResponse } from '@/features/system/mail/types/mailApiTypes'

export const useMailSendQueuesQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.mail.queues.list(),
    queryFn: async () =>
      await get<MailSendQueueResponse[]>('/api/system/mail/queues'),
  })

  return {
    ...query,
    queues: computed(() => query.data.value ?? []),
  }
}