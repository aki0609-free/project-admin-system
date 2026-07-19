import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/master/deduction/api/queryKeys'
import type { DeductionListItemResponse } from '@/features/master/deduction/types/deductionApiTypes'

export const useDeductionsQuery = () => {
  const query = useAppQuery<DeductionListItemResponse[]>({
    queryKey: queryKeys.deductions.list(),
    queryFn: () => get<DeductionListItemResponse[]>('/api/master/deductions'),
  })

  const deductions = computed(() => query.data.value ?? [])

  return {
    ...query,
    deductions,
  }
}