import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/notice/api/queryKeys'
import type { NoticeRuleResponse } from '@/features/system/notice/types/noticeRuleApiTypes'

export const useNoticeRulesQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.noticeRules.list(),
    queryFn: async () =>
      await get<NoticeRuleResponse[]>('/api/system/notice-rules'),
  })

  return {
    ...query,
    rules: computed(() => query.data.value ?? []),
  }
}