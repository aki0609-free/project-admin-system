import { computed, type Ref } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import type { DailyPaymentResponse } from '../types/dailyPaymentApiTypes'
import { dailyPaymentQueryKeys } from './queryKeys'

export const useDailyPaymentsQuery = (
  paymentDate: Ref<string>,
) => {
  const query = useAppQuery({
    queryKey: computed(() => dailyPaymentQueryKeys.list(paymentDate.value)),
    enabled: computed(() => !!paymentDate.value),
    queryFn: async () =>
      await get<DailyPaymentResponse[]>('/api/operation/daily-payments', {
        params: {
          query: {
            paymentDate: paymentDate.value,
          },
        },
      }),
  })

  return {
    ...query,
    payments: computed(() => query.data.value ?? []),
  }
}