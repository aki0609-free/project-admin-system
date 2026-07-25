import { computed } from 'vue'

import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { ruleQueryKeys } from './queryKeys'

export const useRuleBeansQuery = () => {
  const query = useAppQuery({
    queryKey: ruleQueryKeys.ruleBeans.all,
    queryFn: () => get<string[]>('/api/system/rule-beans'),
  })

  const beanNames = computed(() => query.data.value ?? [])

  return {
    ...query,
    beanNames,
  }
}
