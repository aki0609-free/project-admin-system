import { computed, type MaybeRefOrGetter, toValue } from 'vue'

import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import type { PayrollRuleOptionResponse } from '@/features/master/payrollitem/types/payrollRuleOptionTypes'

export const usePayrollRuleOptionsQuery = (
  enabled: MaybeRefOrGetter<boolean>,
  targetType: 'ALLOWANCE' | 'DEDUCTION',
) => {
  const query = useAppQuery<PayrollRuleOptionResponse[]>({
    queryKey: ['master', 'payroll-rule-options', targetType],
    queryFn: () =>
      get<PayrollRuleOptionResponse[]>(
        `/api/master/payroll-rule-options?targetType=${targetType}`,
      ),
    enabled: computed(() => toValue(enabled)),
  })

  const options = computed(() =>
    (query.data.value ?? []).map(rule => ({
      title: `${rule.ruleDisplayName}（${rule.ruleName}）`,
      value: rule.ruleName,
    })),
  )

  return {
    ...query,
    options,
  }
}
