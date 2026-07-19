import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { DeductionDetailTableRow } from '@/features/master/deduction/types/deductionTypes'
import { formatCurrency } from '@/shared/utils/CurrencyUtils'

export const useIncomeTaxDetailConfig = () => {
  const columns = computed<SimpleTableColumnDef<DeductionDetailTableRow>[]>(() => [
    { title: 'ID', key: 'id', width: '180px', filter: { type: 'text' } },
    { title: '年度', key: 'year', width: '180px', filter: { type: 'text' } },
    {
      title: '給与下限',
      key: 'minSalary',
      width: '180px',
      filter: { type: 'text' },
      formatter: value => formatCurrency(value as number),
    },
    {
      title: '給与上限',
      key: 'maxSalary',
      width: '180px',
      filter: { type: 'text' },
      formatter: value => formatCurrency(value as number),
    },
    { title: '扶養人数', key: 'dependents', width: '180px', filter: { type: 'text' } },
    {
      title: '税額',
      key: 'taxAmount',
      width: '180px',
      filter: { type: 'text' },
      formatter: value => formatCurrency(value as number),
    },
    { title: '表示名', key: 'label', width: '220px', filter: { type: 'text' } },
  ])

  const filterRules = computed(() =>
    createSimpleTableFilterRules<DeductionDetailTableRow>(columns.value),
  )

  return {
    title: '所得税詳細',
    description: '所得税は income_tax_table の税額表を参照します。',
    rows: computed<DeductionDetailTableRow[]>(() => []),
    columns,
    filterRules,
  }
}