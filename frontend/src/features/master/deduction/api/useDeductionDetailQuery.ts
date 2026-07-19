import { computed, unref, type MaybeRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/master/deduction/api/queryKeys'
import type { DeductionDetailResponse } from '@/features/master/deduction/types/deductionApiTypes'

export const useDeductionDetailQuery = (
  id: MaybeRef<number | null | undefined>,
) => {
  return useAppQuery<DeductionDetailResponse>({
    queryKey: computed(() => queryKeys.deductions.detail(unref(id) as number)),
    enabled: computed(() => unref(id) != null),
    queryFn: () =>
      get<DeductionDetailResponse>('/api/master/deductions/{id}', {
        params: {
          path: { id: unref(id) as number },
        },
      }),
  })
}