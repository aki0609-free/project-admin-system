import { computed, unref, type MaybeRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/application/api/queryKeys'
import type { ApplicationMediaDetail } from '@/features/application/types/types'

export const useApplicationMediaDetailQuery = (id: MaybeRef<number | null | undefined>) => {
  const query = useAppQuery<ApplicationMediaDetail>({
    queryKey: computed(() => queryKeys.applicationMedias.detail(unref(id) as number)),
    enabled: computed(() => unref(id) != null),
    queryFn: () =>
      get<ApplicationMediaDetail>('/api/application-media/{id}', {
        params: {
          path: { id: unref(id) as number },
        },
      }),
  })

  return query
}