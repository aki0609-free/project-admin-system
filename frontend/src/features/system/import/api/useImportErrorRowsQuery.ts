import { computed, type ComputedRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/import/api/queryKeys'
import type { ImportErrorRowResponse } from '@/features/system/import/types/importApiTypes'

export const useImportErrorRowsQuery = (
  historyId: ComputedRef<number | null>,
) => {
  const query = useAppQuery({
    queryKey: computed(() =>
      queryKeys.importHistories.errors(historyId.value),
    ),
    enabled: computed(() => historyId.value != null),
    queryFn: () =>
      get<ImportErrorRowResponse[]>(
        `/api/system/import-history/${historyId.value}/errors`,
      ),
  })

  const errors = computed(() => query.data.value ?? [])

  return {
    ...query,
    errors,
  }
}