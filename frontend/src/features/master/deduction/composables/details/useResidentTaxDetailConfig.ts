import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { DeductionDetailTableRow } from '@/features/master/deduction/types/deductionTypes'
import { formatCurrency } from '@/shared/utils/CurrencyUtils'

export const useResidentTaxDetailConfig = () => {
  const columns = computed<SimpleTableColumnDef<DeductionDetailTableRow>[]>(() => [
    { title: 'ID', key: 'id', width: '180px', filter: { type: 'text' } },
    { title: '社員ID', key: 'employeeId', width: '180px', filter: { type: 'text' } },
    { title: '年度', key: 'fiscalYear', width: '180px', filter: { type: 'text' } },
    { title: '月', key: 'month', width: '180px', filter: { type: 'text' } },
    {
      title: '住民税額',
      key: 'taxAmount',
      width: '180px',
      filter: { type: 'text' },
      formatter: value => formatCurrency(value as number),
    },
    { title: '表示名', key: 'label', width: '180px', filter: { type: 'text' } },
  ])

  const filterRules = computed(() =>
    createSimpleTableFilterRules<DeductionDetailTableRow>(columns.value),
  )

  return {
    title: '住民税詳細',
    description: '住民税は社員別・年度別・月別の住民税マスターを参照します。',
    rows: computed<DeductionDetailTableRow[]>(() => []),
    columns,
    filterRules,
  }
}