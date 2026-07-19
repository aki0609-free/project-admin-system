import { computed, ref } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { DeductionDetailTableRow } from '@/features/master/deduction/types/deductionTypes'
import { formatCurrency } from '@/shared/utils/CurrencyUtils'
import { formatYearMonthDay } from '@/shared/utils/DateUtils'

export const useAdvancePaymentDetailConfig = () => {
  const rows = ref<DeductionDetailTableRow[]>([
    {
      id: 1,
      employeeId: 101,
      employeeName: '山田 太郎',
      advanceDate: '2026-04-01',
      advanceAmount: 100000,
      repaidAmount: 30000,
      balance: 70000,
      repaymentPerDay: 500,
      status: '返済中',
    },
  ])

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<DeductionDetailTableRow>[] = [
      { title: 'ID', key: 'id', width: '80px', filter: { type: 'text' } },
      { title: '社員ID', key: 'employeeId', width: '100px', filter: { type: 'text' } },
      { title: '社員名', key: 'employeeName', width: '160px', filter: { type: 'text' } },
      {
        title: '前借日',
        key: 'advanceDate',
        width: '150px',
        filter: { type: 'date' },
        formatter: value => formatYearMonthDay(value as string),
      },
      {
        title: '前借金額',
        key: 'advanceAmount',
        width: '140px',
        filter: { type: 'text' },
        formatter: (value, row) => formatCurrency(value as number),
      },
      {
        title: '返済済額',
        key: 'repaidAmount',
        width: '140px',
        filter: { type: 'text' },
        formatter: (value, row) => formatCurrency(value as number),
      },
      {
        title: '残高',
        key: 'balance',
        width: '140px',
        filter: { type: 'text' },
        formatter: (value, row) => formatCurrency(value as number),
      },
      {
        title: '日額返済',
        key: 'repaymentPerDay',
        width: '140px',
        filter: { type: 'text' },
        formatter: (value, row) => formatCurrency(value as number),
      },
      { title: '状態', key: 'status', width: '120px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<DeductionDetailTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
    title: '前借り詳細',
    description: '前借りは社員別の前借りViewを参照します。後で従業員＋前借り情報のViewに接続します。',
  }
}