import { computed, type ComputedRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/batch/api/queryKeys'
import type { BatchJobDefinitionResponse } from '@/features/system/batch/types/batchApiTypes'

export const useBatchJobDefinitionDetailQuery = (
  id: ComputedRef<number | null>,
) => {
  const query = useAppQuery({
    queryKey: computed(() => queryKeys.batch.definitions.detail(id.value)),
    enabled: computed(() => id.value != null),
    queryFn: async () =>
      await get<BatchJobDefinitionResponse>(`/api/system/batch-jobs/${id.value}`),
  })

  return {
    ...query,
    definition: computed(() => query.data.value ?? null),
  }
}