import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import { NoticeResponse } from '../types/dashboardTypes'

export const useNoticesQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.notice.list,
    queryFn: async () =>
      await get<NoticeResponse[]>('/api/notices'),
  })

  return {
    ...query,
    notices: computed(() => query.data.value ?? []),
  }
}