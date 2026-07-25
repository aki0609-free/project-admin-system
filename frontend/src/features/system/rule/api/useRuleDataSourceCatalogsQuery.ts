import { computed } from 'vue'

import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import type { RuleDataSourceCatalog } from '@/features/system/rule/types/ruleApiTypes'
import { ruleQueryKeys } from './queryKeys'

export const useRuleDataSourceCatalogsQuery = () => {
  const query = useAppQuery({
    queryKey: ruleQueryKeys.dataSourceCatalogs.active,
    queryFn: () =>
      get<RuleDataSourceCatalog[]>(
        '/api/system/rule-data-source-catalogs/active',
      ),
  })

  const catalogs = computed(() => query.data.value ?? [])

  return {
    ...query,
    catalogs,
  }
}
