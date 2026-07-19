import { computed, type Ref } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { dailyPaymentQueryKeys } from './queryKeys'
import { type DailyPaymentPrintSummaryResponse } from '../types/dailyPaymentApiTypes'

export const useDailyPaymentPrintSummaryQuery = (paymentDate: Ref<string>) => {
  const query = useAppQuery({
    queryKey: computed(() => dailyPaymentQueryKeys.printSummary(paymentDate.value)),
    enabled: computed(() => !!paymentDate.value),
    queryFn: async () =>
      await get<DailyPaymentPrintSummaryResponse>(
        '/api/operation/daily-payments/print-summary',
        {
          params: {
            query: {
              paymentDate: paymentDate.value,
            },
          },
        },
      ),
  })

  return {
    ...query,
    summary: computed(() => query.data.value ?? null),
  }
}