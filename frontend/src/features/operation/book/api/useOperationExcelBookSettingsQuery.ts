import { computed } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { operationBookQueryKeys } from './queryKeys'
import type { OperationExcelBookSettings } from '../types/operationBookTypes'

export function useOperationExcelBookSettingsQuery() {
  const query = useAppQuery<OperationExcelBookSettings>({
    queryKey: operationBookQueryKeys.settings,
    queryFn: () => get<OperationExcelBookSettings>(
      '/api/operation/excel-books/settings',
    ),
  })

  return {
    ...query,
    settings: computed(() => query.data.value ?? null),
  }
}
