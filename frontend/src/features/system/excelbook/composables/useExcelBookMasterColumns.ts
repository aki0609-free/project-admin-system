import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { ExcelBookMasterResponse } from '../types/excelBookTypes'

export function useExcelBookMasterColumns() {
  const columns = computed<SimpleTableColumnDef<ExcelBookMasterResponse>[]>(() => [
    { title: 'Book Code', key: 'bookCode', width: '180px', filter: { type: 'text' } },
    { title: '名称', key: 'bookName', width: '220px', filter: { type: 'text' } },
    { title: 'Source', key: 'sourceType', width: '120px', filter: { type: 'text' } },
    { title: 'レイアウト', key: 'layoutType', width: '170px', filter: { type: 'text' } },
    { title: 'データソース', key: 'dataSourceCode', width: '260px', filter: { type: 'text' } },
    {
      title: '有効',
      key: 'activeFlag',
      width: '90px',
      filter: { type: 'text' },
      formatter: value => value ? '有効' : '無効',
    },
  ])

  return { columns }
}
