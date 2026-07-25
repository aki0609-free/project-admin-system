import { computed } from 'vue'

import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import type { ImportTargetCatalogResponse } from '@/features/system/import/types/importApiTypes'
import { queryKeys } from './queryKeys'

export const useImportTargetCatalogsQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.importTargetCatalogs.active,
    queryFn: () =>
      get<ImportTargetCatalogResponse[]>(
        '/api/system/import-target-catalogs/active',
      ),
  })

  const catalogs = computed(
    () => query.data.value ?? [],
  )

  return {
    ...query,
    catalogs,
  }
}
