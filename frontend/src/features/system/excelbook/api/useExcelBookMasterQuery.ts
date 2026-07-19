import { computed, type Ref } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import type { ExcelBookMasterResponse } from '../types/excelBookTypes'

export const useExcelBookMasterQuery = (id: Ref<number | null>) => {
  const query = useAppQuery<ExcelBookMasterResponse>({
    queryKey: computed(() => queryKeys.excelBookMasters.detail(id.value)),
    queryFn: () => get<ExcelBookMasterResponse>(`/api/system/excel-book-masters/${id.value}`),
    enabled: computed(() => id.value != null),
  })

  return {
    ...query,
    excelBookMaster: computed(() => query.data.value ?? null),
  }
}