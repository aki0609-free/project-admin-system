import { computed, unref, type MaybeRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/application/api/queryKeys'
import type { ApplicantDetailResponse } from '@/features/application/types/applicantApiTypes'

export const useApplicantDetailQuery = (
  id: MaybeRef<number | null | undefined>,
) => {
  const query = useAppQuery<ApplicantDetailResponse>({
    queryKey: computed(() => queryKeys.applicants.detail(unref(id) as number)),
    enabled: computed(() => unref(id) != null),
    queryFn: () =>
      get<ApplicantDetailResponse>('/api/applicants/{id}', {
        params: {
          path: { id: unref(id) as number },
        },
      }),
  })

  return query
}