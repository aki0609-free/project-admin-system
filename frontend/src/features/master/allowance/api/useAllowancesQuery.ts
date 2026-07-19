import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/master/allowance/api/queryKeys'
import type { AllowanceListItemResponse } from '@/features/master/allowance/types/allowanceApiTypes'

export const useAllowancesQuery = () => {
  const query = useAppQuery<AllowanceListItemResponse[]>({
    queryKey: queryKeys.allowances.list(),
    queryFn: () => get<AllowanceListItemResponse[]>('/api/master/allowances'),
  })

  const allowances = computed(() => query.data.value ?? [])

  return {
    ...query,
    allowances,
  }
}