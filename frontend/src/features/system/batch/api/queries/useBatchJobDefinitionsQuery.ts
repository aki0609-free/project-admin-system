import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/batch/api/queryKeys'
import type { BatchJobDefinitionResponse } from '@/features/system/batch/types/batchApiTypes'

export const useBatchJobDefinitionsQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.batch.definitions.list(),
    queryFn: async () =>
      await get<BatchJobDefinitionResponse[]>('/api/system/batch-jobs'),
  })

  return {
    ...query,
    definitions: computed(() => query.data.value ?? []),
  }
}