import { computed, type ComputedRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/import/api/queryKeys'
import type { ImportTargetResponse } from '@/features/system/import/types/importApiTypes'

export const useImportTargetDetailQuery = (
  id: ComputedRef<number | null>,
) => {
  const query = useAppQuery({
    queryKey: computed(() => queryKeys.importTargets.detail(id.value)),
    enabled: computed(() => id.value != null),
    queryFn: () =>
      get<ImportTargetResponse>(`/api/system/import-targets/${id.value}`),
  })

  const target = computed(() => query.data.value ?? null)

  return {
    ...query,
    target,
  }
}