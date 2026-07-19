import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { ruleQueryKeys } from './queryKeys'
import type { RuleMasterResponse } from '../types/ruleApiTypes'

export const useRulesQuery = () => {
  const query = useAppQuery({
    queryKey: ruleQueryKeys.rules.list,
    queryFn: () => get<RuleMasterResponse[]>('/api/system/rules'),
  })

  const rules = computed(() => query.data.value ?? [])

  return {
    ...query,
    rules,
  }
}