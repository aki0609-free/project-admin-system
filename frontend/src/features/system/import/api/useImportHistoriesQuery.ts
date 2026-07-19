import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/import/api/queryKeys'
import type { ImportHistoryResponse } from '@/features/system/import/types/importApiTypes'

export const useImportHistoriesQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.importHistories.list,
    queryFn: () =>
      get<ImportHistoryResponse[]>('/api/system/import-history'),
  })

  const histories = computed(() => query.data.value ?? [])

  return {
    ...query,
    histories,
  }
}