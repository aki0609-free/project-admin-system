import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/application/api/queryKeys'
import type { ApplicationMediaListItem } from '@/features/application/types/types'

export const useApplicationMediasQuery = () => {
  const query = useAppQuery<ApplicationMediaListItem[]>({
    queryKey: queryKeys.applicationMedias.list(),
    queryFn: () => get<ApplicationMediaListItem[]>('/api/application-media'),
  })

  const applicationMedias = computed(() => query.data.value ?? [])

  return {
    ...query,
    applicationMedias,
  }
}