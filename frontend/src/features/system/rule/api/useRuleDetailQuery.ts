import { computed, type ComputedRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { ruleQueryKeys } from './queryKeys'
import type { RuleMasterResponse } from '../types/ruleApiTypes'

export const useRuleDetailQuery = (
  id: ComputedRef<number | null>,
) => {
  const query = useAppQuery({
    queryKey: computed(() => ruleQueryKeys.rules.detail(id.value)),
    enabled: computed(() => id.value != null),
    queryFn: () => get<RuleMasterResponse>(`/api/system/rules/${id.value}`),
  })

  const rule = computed(() => query.data.value ?? null)

  return {
    ...query,
    rule,
  }
}