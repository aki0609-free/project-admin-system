import { computed, unref, type MaybeRef } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { queryKeys } from './queryKeys'
import type { DailyPreparationResponse } from '../types/dailyPreparationApiTypes'

export const useDailyPreparationQuery = (
  targetDate: MaybeRef<string>,
) => {
  const query = useAppQuery<DailyPreparationResponse | null>({
    queryKey: computed(() =>
      queryKeys.dailyPreparations.byTargetDate(unref(targetDate)),
    ),
    enabled: computed(() => !!unref(targetDate)),
    queryFn: async () =>
      await get<DailyPreparationResponse | null>(
        '/api/operation/daily-preparations',
        {
          params: {
            query: {
              targetDate: unref(targetDate),
            },
          },
        },
      ),
  })

  return {
    ...query,
    preparation: computed(() => query.data.value ?? null),
  }
}