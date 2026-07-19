import { computed, unref, type MaybeRef } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import type { MonthlyClosingSummaryResponse } from '../types/closingApiTypes'
import { queryKeys } from './closingQueryKeys'

export const useClosingSummaryQuery = (
  targetMonth: MaybeRef<string>,
) => {
  const query = useAppQuery<MonthlyClosingSummaryResponse>({
    queryKey: computed(() =>
      queryKeys.closings.summary(unref(targetMonth)),
    ),
    enabled: computed(() => !!unref(targetMonth)),
    queryFn: () =>
      get<MonthlyClosingSummaryResponse>(
        '/api/operation/monthly/summary',
        {
          params: {
            query: {
              targetMonth: unref(targetMonth),
            },
          },
        },
      ),
  })

  return {
    ...query,
    summary: computed(() => query.data.value ?? null),
  }
}