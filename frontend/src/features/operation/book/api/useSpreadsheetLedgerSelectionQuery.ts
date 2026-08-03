import { computed, type Ref } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import type { SpreadsheetLedgerSelectionResponse } from '../types/operationBookTypes'

export function useSpreadsheetLedgerSelectionQuery(
  bookCode: Ref<string | null>,
  targetMonth: Ref<string>,
) {
  const query = useAppQuery<SpreadsheetLedgerSelectionResponse>({
    queryKey: computed(() => [
      'operationExcelBooks',
      'selection',
      bookCode.value,
      targetMonth.value,
    ]),
    enabled: computed(() => !!bookCode.value && !!targetMonth.value),
    queryFn: () => get<SpreadsheetLedgerSelectionResponse>(
      `/api/operation/excel-books/${bookCode.value}/selection-options`
        + `?targetMonth=${encodeURIComponent(targetMonth.value)}`,
    ),
  })

  return {
    ...query,
    selection: computed(() => query.data.value ?? null),
  }
}
