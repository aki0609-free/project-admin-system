import { computed, unref, type MaybeRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/master/allowance/api/queryKeys'
import type { AllowanceDetailResponse } from '@/features/master/allowance/types/allowanceApiTypes'

export const useAllowanceDetailQuery = (
  id: MaybeRef<number | null | undefined>,
) => {
  return useAppQuery<AllowanceDetailResponse>({
    queryKey: computed(() => queryKeys.allowances.detail(unref(id) as number)),
    enabled: computed(() => unref(id) != null),
    queryFn: () =>
      get<AllowanceDetailResponse>('/api/master/allowances/{id}', {
        params: {
          path: { id: unref(id) as number },
        },
      }),
  })
}