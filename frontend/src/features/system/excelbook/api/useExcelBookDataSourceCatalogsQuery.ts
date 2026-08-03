import { computed } from 'vue'

import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import type { ExcelBookDataSourceCatalog } from '../types/excelBookTypes'
import { queryKeys } from './queryKeys'

export const useExcelBookDataSourceCatalogsQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.dataSourceCatalogs.active,
    queryFn: () =>
      get<ExcelBookDataSourceCatalog[]>(
        '/api/system/excel-book-data-source-catalogs/active',
      ),
  })

  const catalogs = computed(() => query.data.value ?? [])

  return {
    ...query,
    catalogs,
  }
}
