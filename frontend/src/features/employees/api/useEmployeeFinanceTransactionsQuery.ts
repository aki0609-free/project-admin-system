import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import type { EmployeeFinanceTransactionResponse } from '../types/employeeWorkApiTypes'

export const useEmployeeFinanceTransactionsQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.employeeWork.financeTransactions.list(),
    queryFn: async () =>
      await get<EmployeeFinanceTransactionResponse[]>('/api/employee/finance-transactions'),
  })

  return {
    ...query,
    transactions: computed(() => query.data.value ?? []),
  }
}
