import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { DeductionDetailTableRow } from '@/features/master/deduction/types/deductionTypes'
import { formatCurrency } from '@/shared/utils/CurrencyUtils'

export const useStandardSalaryDetailConfig = () => {
  const columns = computed<SimpleTableColumnDef<DeductionDetailTableRow>[]>(() => [
    { title: 'ID', key: 'id', width: '180px', filter: { type: 'text' } },
    {
      title: '報酬下限',
      key: 'minSalary',
      width: '180px',
      filter: { type: 'text' },
      formatter: value => formatCurrency(value as number),
    },
    {
      title: '報酬上限',
      key: 'maxSalary',
      width: '180px',
      filter: { type: 'text' },
      formatter: value => formatCurrency(value as number),
    },
    {
      title: '標準報酬月額',
      key: 'standardSalary',
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
    title: '標準報酬月額詳細',
    description: '標準報酬月額テーブルを参照します。',
    rows: computed<DeductionDetailTableRow[]>(() => []),
    columns,
    filterRules,
  }
}