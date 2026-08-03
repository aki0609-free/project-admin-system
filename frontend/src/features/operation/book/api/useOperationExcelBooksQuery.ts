import { computed } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { operationBookQueryKeys } from './queryKeys'
import type { OperationExcelBook } from '../types/operationBookTypes'

export function useOperationExcelBooksQuery() {
  const query = useAppQuery<OperationExcelBook[]>({
    queryKey: operationBookQueryKeys.active,
    queryFn: () =>
      get<OperationExcelBook[]>('/api/operation/excel-books'),
  })

  return {
    ...query,
    books: computed(() => query.data.value ?? []),
  }
}
