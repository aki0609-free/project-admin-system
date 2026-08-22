import { computed, unref, type MaybeRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/master/deduction/api/queryKeys'
import type { DeductionDetailResponse } from '@/features/master/deduction/types/deductionApiTypes'

export const useDeductionDetailQuery = (
  id: MaybeRef<number | null | undefined>,
  targetDate: MaybeRef<string>,
) => {
  return useAppQuery<DeductionDetailResponse>({
    queryKey: computed(() =>
      queryKeys.deductions.detailAt(
        unref(id) as number,
        unref(targetDate),
      ),
    ),
    enabled: computed(() => unref(id) != null && unref(targetDate).length > 0),
    queryFn: () =>
      get<DeductionDetailResponse>('/api/master/deductions/{id}', {
        params: {
          path: { id: unref(id) as number },
          query: { targetDate: unref(targetDate) },
        },
      }),
  })
}
