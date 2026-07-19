import { computed, type Ref } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import { NoticeResponse } from '../types/dashboardTypes'

export const useNoticeCalendarQuery = (
  from: Ref<string>,
  to: Ref<string>,
) => {
  const query = useAppQuery({
    queryKey: computed(() =>
      queryKeys.notice.calendar(from.value, to.value),
    ),
    queryFn: async () =>
      await get<NoticeResponse[]>('/api/notices/calendar', {
        params: {
          query: {
            from: from.value,
            to: to.value,
          },
        },
      }),
  })

  return {
    ...query,
    notices: computed(() => query.data.value ?? []),
  }
}