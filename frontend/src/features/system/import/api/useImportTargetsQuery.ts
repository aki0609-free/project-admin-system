import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/import/api/queryKeys'
import type { ImportTargetResponse } from '@/features/system/import/types/importApiTypes'

export const useImportTargetsQuery = (activeOnly = false) => {
  const query = useAppQuery({
    queryKey: activeOnly ? queryKeys.importTargets.active : queryKeys.importTargets.list,
    queryFn: () =>
      get<ImportTargetResponse[]>(
        activeOnly ? '/api/system/import-targets/active' : '/api/system/import-targets',
      ),
  })

  const targets = computed(() => query.data.value ?? [])

  return {
    ...query,
    targets,
  }
}