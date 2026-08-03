import { computed, type Ref } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { queryKeys } from './queryKeys'
import type { SpreadsheetTemplateResponse } from '../types/excelBookTypes'

export const useSpreadsheetTemplateQuery = (
  masterId: Ref<number | null>,
  enabled: Ref<boolean>,
) => {
  const query = useAppQuery<SpreadsheetTemplateResponse>({
    queryKey: computed(() =>
      queryKeys.spreadsheetTemplates.detail(masterId.value),
    ),
    queryFn: () =>
      get<SpreadsheetTemplateResponse>(
        `/api/system/excel-book-masters/${masterId.value}/spreadsheet-template`,
      ),
    enabled: computed(() => enabled.value && masterId.value != null),
  })

  return {
    ...query,
    template: computed(() => query.data.value ?? null),
  }
}
