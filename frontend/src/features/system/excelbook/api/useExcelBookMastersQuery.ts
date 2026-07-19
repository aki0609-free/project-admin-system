import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from './queryKeys'
import type { ExcelBookMasterResponse } from '../types/excelBookTypes'

export const useExcelBookMastersQuery = () => {
  const query = useAppQuery<ExcelBookMasterResponse[]>({
    queryKey: queryKeys.excelBookMasters.list(),
    queryFn: () => get<ExcelBookMasterResponse[]>('/api/system/excel-book-masters'),
  })

  const excelBookMasters = computed(() => query.data.value ?? [])

  return {
    ...query,
    excelBookMasters,
  }
}