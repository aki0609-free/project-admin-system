import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'
import type { MailTemplateResponse } from '@/features/system/mail/types/mailApiTypes'

export const useMailTemplatesQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.mail.templates.list(),
    queryFn: async () =>
      await get<MailTemplateResponse[]>('/api/system/mail-templates'),
  })

  return {
    ...query,
    templates: computed(() => query.data.value ?? []),
  }
}
