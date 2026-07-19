import { computed, type Ref } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/batch/api/queryKeys'
import type { BatchExecutionLogResponse } from '@/features/system/batch/types/batchApiTypes'

export const useBatchExecutionLogsQuery = (
  jobCode?: Ref<string | null>,
) => {
  const query = useAppQuery({
    queryKey: computed(() =>
      jobCode?.value
        ? queryKeys.batch.logs.byJobCode(jobCode.value)
        : queryKeys.batch.logs.list(),
    ),
    queryFn: async () => {
      if (jobCode?.value) {
        return await get<BatchExecutionLogResponse[]>(
          `/api/system/batch/logs/${encodeURIComponent(jobCode.value)}`,
        )
      }

      return await get<BatchExecutionLogResponse[]>('/api/system/batch/logs')
    },
  })

  return {
    ...query,
    logs: computed(() => query.data.value ?? []),
  }
}