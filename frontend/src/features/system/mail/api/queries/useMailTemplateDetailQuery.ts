import { computed, type ComputedRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'
import type { MailTemplateResponse } from '@/features/system/mail/types/mailApiTypes'

export const useMailTemplateDetailQuery = (
  id: ComputedRef<number | null>,
) => {
  const query = useAppQuery({
    queryKey: computed(() => queryKeys.mail.templates.detail(id.value)),
    enabled: computed(() => id.value != null),
    queryFn: async () =>
      await get<MailTemplateResponse>(`/api/system/mail-templates/${id.value}`),
  })

  return {
    ...query,
    template: computed(() => query.data.value ?? null),
  }
}
