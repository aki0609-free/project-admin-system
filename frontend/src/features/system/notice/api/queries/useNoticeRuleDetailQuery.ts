import { computed, type ComputedRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/notice/api/queryKeys'
import type { NoticeRuleResponse } from '@/features/system/notice/types/noticeRuleApiTypes'

export const useNoticeRuleDetailQuery = (
  id: ComputedRef<number | null>,
) => {
  const query = useAppQuery({
    queryKey: computed(() => queryKeys.noticeRules.detail(id.value)),
    enabled: computed(() => id.value != null),
    queryFn: async () =>
      await get<NoticeRuleResponse>(`/api/system/notice-rules/${id.value}`),
  })

  return {
    ...query,
    rule: computed(() => query.data.value ?? null),
  }
}